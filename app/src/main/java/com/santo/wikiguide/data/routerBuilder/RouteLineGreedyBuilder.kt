package com.santo.wikiguide.data.routerBuilder

import com.mapbox.geojson.Point

class RouteLineGreedyBuilder {
    fun getRoute(timeLimit:Double, points:List<Point>, resultListener: RouteCircleGreedyBuilder.routeResultListener){
        getMatrix(points){
            val result=getOrder(timeLimit,points)
            val route=ArrayList<Point>()
            for(i in 0 until result.first.size){
                route.add(points[result.first[i]])
            }
            resultListener.onRouteResult(Pair(route,result.second))
        }
    }
}