package com.wayne.hyperaicrbypass.hook;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class GlobalProgressRequestCollector {
    private static final int GLOBAL_SCOPE = 31;

    private final AtomicLong generations = new AtomicLong();
    private final ThreadLocal<Deque<Request>> requests =
            ThreadLocal.withInitial(ArrayDeque::new);

    public IndexToken beginIndex(int scope, boolean cache) {
        if (scope != GLOBAL_SCOPE) {
            return null;
        }
        Request request = new Request(generations.incrementAndGet(), cache);
        requests.get().push(request);
        return new IndexToken(request);
    }

    public ScopeToken beginScope(int scope) {
        Request request = current();
        if (request == null || !isLocalScope(scope)) {
            return null;
        }
        ScopeFrame frame = new ScopeFrame(scope);
        request.scopes.push(frame);
        return new ScopeToken(request, frame);
    }

    public void captureLocal(Object[] args) {
        Request request = current();
        if (request == null || request.scopes.isEmpty()
                || args == null || args.length != 3) {
            return;
        }
        int[] counts = integers(args);
        if (counts != null) {
            request.scopes.peek().counts = counts;
        }
    }

    public void finishScope(ScopeToken token, Object result) {
        if (token == null) {
            return;
        }
        Request request = current();
        if (request != token.request || request.scopes.peek() != token.frame) {
            return;
        }
        ScopeFrame frame = request.scopes.pop();
        if (frame.counts == null || !(result instanceof Integer fixed)) {
            return;
        }
        GlobalProgressComponent.fromLocalCounts(
                frame.counts[0], frame.counts[1], frame.counts[2], fixed
        ).ifPresent(component -> request.components.put(frame.scope, component));
    }

    public GalleryToken beginGallery() {
        Request request = current();
        if (request == null) {
            return null;
        }
        GalleryFrame frame = new GalleryFrame();
        request.galleries.push(frame);
        return new GalleryToken(request, frame);
    }

    public void captureGallery(Object[] args, Object result) {
        Request request = current();
        if (request == null || request.galleries.isEmpty()) {
            return;
        }
        PreciseProgressHookLogic.snapshotFromCalculator(args, result, 0L)
                .flatMap(snapshot -> GlobalProgressComponent.restore(
                        snapshot.numerator(),
                        snapshot.denominator(),
                        snapshot.fixedProgress()
                ))
                .ifPresent(component -> request.galleries.peek().candidate = component);
    }

    public void captureMigrationPostprocess(Object[] args, Object result) {
        Request request = current();
        if (request == null || request.galleries.isEmpty()
                || args == null || args.length != 5
                || !(args[0] instanceof Float galleryAppProgress)
                || !(args[1] instanceof Integer aiProgress)
                || !(args[2] instanceof Integer migratedCount)
                || !(args[3] instanceof Integer mediaCountBefore)
                || !(args[4] instanceof Integer mediaCountCurrent)
                || !(result instanceof Integer observedGalleryProgress)) {
            return;
        }
        GalleryFrame frame = request.galleries.peek();
        if (frame.candidate == null || frame.candidate.fixedProgress() != aiProgress) {
            return;
        }
        frame.postprocess = new Postprocess(
                frame.candidate,
                galleryAppProgress,
                migratedCount,
                mediaCountBefore,
                mediaCountCurrent,
                observedGalleryProgress
        );
    }

    public void finishGallery(GalleryToken token, Object result) {
        if (token == null) {
            return;
        }
        Request request = current();
        if (request != token.request || request.galleries.peek() != token.frame) {
            return;
        }
        GalleryFrame frame = request.galleries.pop();
        if (!(result instanceof Integer fixed)) {
            return;
        }
        if (frame.postprocess != null
                && frame.postprocess.observedGalleryProgress == fixed) {
            request.postprocess = frame.postprocess;
            request.gallery = frame.postprocess.galleryAi;
        } else if (frame.candidate != null
                && frame.candidate.fixedProgress() == fixed) {
            request.gallery = frame.candidate;
        }
    }

    public void markMigratedDirect() {
        Request request = current();
        if (request != null) {
            request.branch = GlobalProgressBranch.MIGRATED_DIRECT_AI;
        }
    }

    public void markUnmigratedLocal() {
        Request request = current();
        if (request != null) {
            request.branch = GlobalProgressBranch.UNMIGRATED_LOCAL;
        }
    }

    public Optional<GlobalProgressSnapshot> finishIndex(
            IndexToken token,
            Object result,
            long runStartTime,
            long capturedElapsedRealtime
    ) {
        if (token == null) {
            return Optional.empty();
        }
        Deque<Request> stack = requests.get();
        Request request = stack.peek();
        try {
            if (request != token.request
                    || request.cache
                    || request.branch == null
                    || !(result instanceof Integer fixed)) {
                return Optional.empty();
            }
            if (request.branch == GlobalProgressBranch.UNMIGRATED_LOCAL) {
                return GlobalProgressMath.unmigratedLocal(
                        request.components.get(2),
                        request.components.get(4),
                        request.components.get(8),
                        request.components.get(16),
                        fixed,
                        runStartTime,
                        request.generation,
                        capturedElapsedRealtime
                );
            }
            if (request.postprocess != null) {
                Postprocess postprocess = request.postprocess;
                return GlobalProgressMath.migratedPostprocessed(
                        postprocess.galleryAi,
                        postprocess.galleryAppProgress,
                        postprocess.migratedCount,
                        postprocess.mediaCountBefore,
                        postprocess.mediaCountCurrent,
                        postprocess.observedGalleryProgress,
                        request.components.get(2),
                        request.components.get(4),
                        request.components.get(8),
                        request.components.get(16),
                        fixed,
                        runStartTime,
                        request.generation,
                        capturedElapsedRealtime
                );
            }
            return GlobalProgressMath.migratedDirect(
                    request.gallery,
                    request.components.get(2),
                    request.components.get(4),
                    request.components.get(8),
                    request.components.get(16),
                    fixed,
                    runStartTime,
                    request.generation,
                    capturedElapsedRealtime
            );
        } finally {
            if (stack.peek() == token.request) {
                stack.pop();
            }
            if (stack.isEmpty()) {
                requests.remove();
            }
        }
    }

    boolean hasActiveRequest() {
        return current() != null;
    }

    private Request current() {
        return requests.get().peek();
    }

    private static boolean isLocalScope(int scope) {
        return scope == 2 || scope == 4 || scope == 8 || scope == 16;
    }

    private static int[] integers(Object[] args) {
        int[] values = new int[args.length];
        for (int index = 0; index < args.length; index++) {
            if (!(args[index] instanceof Integer value)) {
                return null;
            }
            values[index] = value;
        }
        return values;
    }

    public static final class IndexToken {
        private final Request request;

        private IndexToken(Request request) {
            this.request = request;
        }
    }

    public static final class ScopeToken {
        private final Request request;
        private final ScopeFrame frame;

        private ScopeToken(Request request, ScopeFrame frame) {
            this.request = request;
            this.frame = frame;
        }
    }

    public static final class GalleryToken {
        private final Request request;
        private final GalleryFrame frame;

        private GalleryToken(Request request, GalleryFrame frame) {
            this.request = request;
            this.frame = frame;
        }
    }

    private static final class Request {
        private final long generation;
        private final boolean cache;
        private final Map<Integer, GlobalProgressComponent> components = new HashMap<>();
        private final Deque<ScopeFrame> scopes = new ArrayDeque<>();
        private final Deque<GalleryFrame> galleries = new ArrayDeque<>();
        private GlobalProgressComponent gallery;
        private GlobalProgressBranch branch;
        private Postprocess postprocess;

        private Request(long generation, boolean cache) {
            this.generation = generation;
            this.cache = cache;
        }
    }

    private static final class ScopeFrame {
        private final int scope;
        private int[] counts;

        private ScopeFrame(int scope) {
            this.scope = scope;
        }
    }

    private static final class GalleryFrame {
        private GlobalProgressComponent candidate;
        private Postprocess postprocess;
    }

    private record Postprocess(
            GlobalProgressComponent galleryAi,
            float galleryAppProgress,
            int migratedCount,
            int mediaCountBefore,
            int mediaCountCurrent,
            int observedGalleryProgress
    ) {
    }
}
