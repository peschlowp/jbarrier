jbarrier - High performance barrier synchronization for Java
============================================================

The jbarrier library provides a set of barrier algorithms for thread synchronization in Java. Compared to the CyclicBarrier shipped with Java SE, the jbarrier algorithms show superior performance for a number of use cases. Additionally, the jbarrier algorithms support global reductions, i.e., reducing data contributed by the threads into a single result while the barrier synchronization is done.

Background
==========

While developing a parallel wireless network simulator in Java, we were in need of a barrier construct to repeatedly synchronize a number of threads participating in the simulation. We ran some tests using the CyclicBarrier from the java.util.concurrent package, but it turned out that the synchronization overhead was much higher than what was desired for our application. Therefore, we started to experiment with self-written implementations of various barrier algorithms and found that our simulator showed much better performance when using our own implementations.

Eventually, we decided to extract the barrier implementations from our simulator and put them into a separate library, jbarrier, so that they can be used by other applications, too. In the process of creating the library, we added many optimizations and extensions to the different barrier implementations, and there may be more to come.

Performance
===========

In the project wiki, you can find the results of a performance study with jbarrier.

Usage
=====

If you only want to use jbarrier in your Java application, extracting the main jar "jbarrier-1.0.jar" from the distribution and adding it to your classpath is all you need to do. Examples of how to use the barrier implementations are given in the package peschlowp.jbarrier.examples. Developers who want to make changes to jbarrier can easily build modified versions of jbarrier using the ant build file.

Limitations
===========

It is important to note that the jbarrier algorithms have been written for applications where the number of threads participating in the barrier synchronizations is not larger than the number of physical cores of the machine. A typical application of that kind is an expensive computation split into a pre-defined number of worker threads that have to synchronize frequently in order to exchange intermediate results. Having such applications in mind, our implementation makes heavy use of active waiting (spinning on atomic and volatile variables, etc.). Thus, its performance may decline severely if the number of threads participating in the barrier is higher than the number of physical cores.

License
=======

The library is licensed under the Apache License, Version 2.0.
