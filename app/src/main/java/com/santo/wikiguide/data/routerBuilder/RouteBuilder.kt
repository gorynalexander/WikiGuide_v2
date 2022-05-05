package com.santo.wikiguide.data.routerBuilder

import com.mapbox.geojson.Point

abstract class RouteBuilder {
    val INF=20000.0

    open fun getRoute(timeLimit:Double, startPoint: Point, points:List<Point>, circleResultListener: routeCircleResultListener){}
    open fun getRoute(distanceThreshold:Double, startPoint: Point, endPoint:Point, points:List<Point>, lineResultListener: routeLineResultListener) {}

    open fun getOrder(timeLimit:Double, points: List<Point>): Pair<ArrayList<Int>, Double>{
        return Pair(ArrayList(),0.0)
    }
    open fun getOrder(distanceThreshold:Double,distances: Pair<List<Double>,List<Double>>, points: List<Point>): ArrayList<Int>{
        return ArrayList<Int>()
    }

    open fun getScore(point: Point): Double {
//        TODO("Not yet implemented")
        return 1.0
    }

    fun interface routeCircleResultListener{
        fun onRouteResult(result: Pair<ArrayList<Point>, Double> )
    }
    fun interface routeLineResultListener{
        fun onRouteResult(result: ArrayList<Point>)
    }
}