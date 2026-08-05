package com.wayne.hyperaicrbypass.hook;

public final class RescanGenerationGate {
    private long generation;

    public RescanGenerationGate(long initialGeneration) {
        generation = initialGeneration;
    }

    public synchronized boolean tryAdvance(long candidate) {
        if (candidate <= generation) {
            return false;
        }
        generation = candidate;
        return true;
    }
}
