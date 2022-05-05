package com.santo.wikiguide.data.routerBuilder

import com.mapbox.geojson.Point
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

class RouteLineGreedy:RouteBuilder() {
    val weightScore = 10000
    val weightDistance = 1
    val mPerLongitudeDegree=111134.861111
    val mPerLatitudeDegreeOnEquator=111321.377778

    var N=0

/*distanceThreshold in meters*/
    override
    fun getRoute(
        distanceThreshold: Double,
        startPoint: Point,
        endPoint: Point,
        points: List<Point>,
        lineResultListener: routeLineResultListener
    ) {
        N = points.size
        getDistances(startPoint,endPoint,points){
            distances->
            val resultOrder = getOrder(distanceThreshold,distances, points)
            val route = ArrayList<Point>()
            route.add(startPoint)
            for (i in 0 until resultOrder.size) {
                route.add(points[resultOrder[i]])
            }
            route.add(endPoint)
            lineResultListener.onRouteResult(route)
        }
    }

    override
    fun getOrder(distanceThreshold: Double, distances:Pair<List<Double>,List<Double>>, points: List<Point>): ArrayList<Int> {
        val scores = ArrayList<Double>()
//TODO change to POI
        for (point in points) {
            scores.add(getScore(point))
        }
        val distancesToLine=distances.first
        val distancesToStart=distances.second
        for(i in 0 until scores.size){
            if(distancesToLine[i]>distanceThreshold || distancesToLine[i]==INF){
                scores[i]=-INF
            }
            else{
                scores[i]=scores[i]*weightScore-weightDistance*distancesToLine[i]
            }
        }
        val chosenPoints = ArrayList<Int>()

        for(i in 0 until scores.size){
            val maxScore= Collections.max(scores)
            if(maxScore==-INF)
                break
            val maxIndex=scores.indexOf(maxScore)
            chosenPoints.add(maxIndex)
            scores[maxIndex]=-INF
        }
//        optimize(chosenPoints){}
        val order = ArrayList<Int>()
        val mutableDistancesToStart=ArrayList<Double>()
        for(i in 0 until N) {
            mutableDistancesToStart.add(distancesToStart[i])
        }
        for(i in 0 until N) {
            val minDistance= Collections.min(mutableDistancesToStart)
            if(minDistance==INF)
                break
            val minIndex=mutableDistancesToStart.indexOf(minDistance)
            if(chosenPoints.contains(minIndex)){
                order.add(minIndex)
            }
            mutableDistancesToStart[minIndex]=INF
        }
        return order
    }

////       TODO: change hardcode
//    private fun optimize(points: List<Point>, optimizeListener:OptimizeListener) {
//        val optimizationApiClient= MapboxOptimization.builder()
//            .accessToken("pk.eyJ1Ijoia3lyeWx2ZXJlbWlvdiIsImEiOiJjbDEyNGhvY2gwM251M2tzMDhlOHE1aXAxIn0.r_PHqSSTrolmC5HBShznQw")
//            .coordinates(points)
//            .profile(DirectionsCriteria.PROFILE_WALKING)
//            .build()
//        optimizationApiClient.enqueueCall(object : Callback<OptimizationResponse>{
//            override fun onResponse(
//                call: Call<OptimizationResponse>,
//                response: Response<OptimizationResponse>
//            ) {
//
//                Timber.i("Getting optimized route success")
//                optimizeListener.onOptimizeResult(response.body()?.trips())
//            }
//
//            override fun onFailure(call: Call<OptimizationResponse>, t: Throwable) {
//                Timber.i("Getting optimized route failed")
//            }
//
//        })
//    }

    fun getDistances(startPoint: Point,endPoint: Point,
        points: List<Point>,
        resultListener: DistancesResultListener
    ) {
        val distancesToLine=ArrayList<Double>()

        val Ox=startPoint.longitude()
        val Oy=startPoint.latitude()

        val X0=0
        val Y0=0
        val X1=(endPoint.longitude()-Ox)*mPerLongitudeDegree
        val Y1=(endPoint.latitude()-Oy)*(cos(degreesToRad(endPoint.latitude()))*mPerLatitudeDegreeOnEquator)
        val A=Y1-Y0
        val B=-(X1-X0)
        val C=-(A*X0+B*Y0)
        val sqrtAB= kotlin.math.sqrt(A*A+B*B)
        assert(sqrtAB!=0.0)
        val Mx=ArrayList<Double>()
        val My=ArrayList<Double>()
        for (i in 0 until N){
            Mx.add((points[i].longitude()-Ox)*mPerLongitudeDegree)
            My.add((points[i].latitude()-Oy)*(cos(degreesToRad(points[i].latitude()))*mPerLatitudeDegreeOnEquator))
            if((((X1-X0)*(Mx.last()-X0)+(Y1-Y0)*(My.last()-Y0))>0)&&(((X0-X1)*(Mx.last()-X1)+(Y0-Y1)*(My.last()-Y1))>0)){
                distancesToLine.add(abs(A*Mx.last()+B*My.last()+C)/sqrtAB)
            }
            else{
                distancesToLine.add(INF)
            }
        }
        val distancesToStart=ArrayList<Double>()
        for(i in 0 until N){
            distancesToStart.add(sqrt((X0-Mx[i])*(X0-Mx[i])+(Y0-My[i])*(Y0-My[i])))
        }
        resultListener.onDistanceResult(Pair(distancesToLine,distancesToStart))
    }

    private fun degreesToRad(degree: Double): Double {
        return degree* PI/180
    }

    fun interface DistancesResultListener{
        fun onDistanceResult(results: Pair<List<Double>, List<Double>>)
    }

//    fun interface OptimizeListener{
//        fun onOptimizeResult(results: MutableList<DirectionsRoute>?)
//    }
}