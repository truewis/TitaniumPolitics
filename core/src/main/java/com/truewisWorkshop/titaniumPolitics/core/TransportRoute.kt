package com.titaniumPolitics.game.core

/**
 * Describes one hop of a transport route: from one place to an adjacent place,
 * the best transport apparatus (or null for manual carry), and the segment throughput.
 */
data class TransportSegment(
    val fromPlace: String,
    val toPlace: String,
    /** Display name of the transport method used on this segment. */
    val methodName: String,
    /** Throughput for the resource on this segment (units/hr). */
    val throughput: Double
)

/**
 * A complete transport route from source to destination for a given resource.
 *
 * @param segments  Ordered list of hops in the route.
 * @param bottleneckThroughput  The minimum throughput across all segments; determines
 *                              how fast resources can actually be moved end-to-end.
 */
data class TransportRoute(
    val segments: List<TransportSegment>,
    val bottleneckThroughput: Double
)
