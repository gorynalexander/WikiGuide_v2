package com.santo.wikiguide.data.routerBuilder

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.matrix.v1.MapboxMatrix
import com.mapbox.api.matrix.v1.models.MatrixResponse
import com.mapbox.geojson.Point
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class RouteCircleGreedy : RouteBuilder() {
    val weightScore=10000
    val weightTime=1

    lateinit var durationsMatrix: List<Array<Double>>

    override
    fun getRoute(timeLimit:Double, startPoint:Point, pointsToVisit:List<Point>, circleResultListener: routeCircleResultListener){
        val points= pointsToVisit.plus(startPoint)
        Collections.reverse(points)
        getMatrix(points){
            val result=getOrder(timeLimit,points)
            val route=ArrayList<Point>()
            for(i in 0 until result.first.size){
                route.add(points[result.first[i]])
            }
            circleResultListener.onRouteResult(Pair(route,result.second))
        }
    }

    override
    fun getOrder(timeLimit:Double, points: List<Point>): Pair<ArrayList<Int>, Double> {
        val N=points.size
        val scores=ArrayList<Double>()
//TODO change to POI
        for (point in points){
            scores.add(getScore(point))
        }
        val weightMatrix=ArrayList<ArrayList<Pair<Int,Double>>>()
        for (i in 0 until N){
            weightMatrix.add(ArrayList())
            for (j in 0 until N){
                if(i==j)
                    weightMatrix[i].add(Pair(j,-INF))
                else
                    weightMatrix[i].add(Pair(j,scores[j]*weightScore-weightTime*durationsMatrix[i][j]))
            }
        }
        for(i in 0 until N){
            for(j in 0 until N-1){
                for (k in 0 until N-j-1){
                    if(weightMatrix[i][k].second<weightMatrix[i][k+1].second) {
                        val tmp=weightMatrix[i][k]
                        weightMatrix[i][k]=weightMatrix[i][k+1]
                        weightMatrix[i][k+1]=tmp
                    }
                }
            }
        }
        val was=HashSet<Int>()
        val order=ArrayList<Int>()
        var time=0.0
        var I=0
        was.add(I)
        order.add(I)
        for(i in 0 until N){
            var j=0
            while(j<N&&(was.contains(weightMatrix[I][j].first)||(durationsMatrix[I][weightMatrix[I][j].first]+time>timeLimit)))
                j++
            if(j!=N){
                time += durationsMatrix[I][weightMatrix[I][j].first]
                order.add(weightMatrix[I][j].first)
                was.add(weightMatrix[I][j].first)
                I=weightMatrix[I][j].first
            }
            else
                break
        }
        return Pair(order,time)
   }
    fun getMatrix(points:List<Point>, resultListener: matrixResultListener){

//       TODO: change hardcode
        val matrixApiClient= MapboxMatrix.builder()
            .accessToken("pk.eyJ1Ijoia3lyeWx2ZXJlbWlvdiIsImEiOiJjbDEyNGhvY2gwM251M2tzMDhlOHE1aXAxIn0.r_PHqSSTrolmC5HBShznQw")
            .coordinates(points)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .build()
        matrixApiClient.enqueueCall(object : Callback<MatrixResponse> {
            override fun onResponse(call: Call<MatrixResponse>, response: Response<MatrixResponse>) {
                durationsMatrix= response.body()?.durations()!!
                Timber.i("Getting Matrix success")
                resultListener.onMatrixResult(durationsMatrix)
            }

            override fun onFailure(call: Call<MatrixResponse>, throwable: Throwable) {
                Timber.i("Getting Matrix failed")
            }
        })
    }

    fun interface matrixResultListener{
        fun onMatrixResult(results: List<Array<Double>>)
    }

}