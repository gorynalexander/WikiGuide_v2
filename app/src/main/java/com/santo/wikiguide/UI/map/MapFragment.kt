package com.santo.wikiguide.UI.map

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.gestures.*
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.search.result.SearchResult
import com.santo.wikiguide.R
import com.santo.wikiguide.data.routerBuilder.RouteLineGreedy
import com.santo.wikiguide.databinding.FragmentMapBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.roundToInt


@MapboxExperimental
@AndroidEntryPoint
class MapFragment : Fragment(),OnMapClickListener  {

    private lateinit var binding: FragmentMapBinding
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var viewAnnotationManager: ViewAnnotationManager


    private lateinit var mapboxNavigation: MapboxNavigation
    private val replayLocationEngine = ReplayLocationEngine(MapboxReplayer())
    private lateinit var onIndicatorPositionChangedListener:OnIndicatorPositionChangedListener

    private val viewModel: MapFragmentViewModel by viewModels()
    private val points:ArrayList<Point> =ArrayList()


    private var markerWidth = 0
    private var markerHeight = 0
    private val asyncInflater by lazy { context?.let { AsyncLayoutInflater(it) } }

    private lateinit var route:List<Point>
    private var durationWalk=ArrayList<Double>()
    var I=0

    var firstUpdate=true

    private lateinit var destinationPoint: Point

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentMapBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initNavigation()
        initMap()

        binding.inputCategoryName.setText("cafe")
        binding.inputCategoryLimit.setText("5")

        binding.addButton.setOnClickListener {
            viewModel.getPOIs(binding.inputCategoryName.text.toString(),binding.inputCategoryLimit.text.toString().toInt())
        }
    }



    private fun initMap(){
        mapView = binding.mapView
        viewAnnotationManager = binding.mapView.viewAnnotationManager


        var bitmap = BitmapFactory.decodeResource(resources, R.drawable.blue_marker)
        bitmap= Bitmap.createScaledBitmap(bitmap,bitmap.width/2,bitmap.height/2,false)
        markerWidth = bitmap.width
        markerHeight = bitmap.height
        mapboxMap = binding.mapView.getMapboxMap().apply {
            loadStyle(
                styleExtension = prepareStyle(Style.MAPBOX_STREETS, bitmap)
            ) {

                mapView.location.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                }
                addOnMapClickListener(this@MapFragment)
//                addOnMapLongClickListener(this@MapFragment)
//                Toast.makeText(this@MapFragment, STARTUP_TEXT, Toast.LENGTH_LONG).show()
            }
        }

//        addOnMapClickListener(this@MapFragment)
//        addOnMapLongClickListener(this@MapFragment)



        // Get the user's location as coordinates
//      Round's screen to the direction of user's look
//      Problem: always round back

//        val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
//            mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
//        }

        onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
            mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
            viewModel.currentLocation=it
            if(firstUpdate){
                firstUpdate=false
                addObserver()
            }
        }

// Pass the user's location to camera
        mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
//        mapView.location.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)


        mapView.gestures.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
                mapView.location
                    .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            }

            override fun onMove(detector: MoveGestureDetector): Boolean {
                return false
            }

            override fun onMoveEnd(detector: MoveGestureDetector) {}
        })

        mapView.gestures.addOnMapLongClickListener{
            drawRoute(listOf(viewModel.currentLocation,destinationPoint))
            drawRoute(route)
            true
        }
    }

    fun addObserver(){
        viewModel.poiList.observe(viewLifecycleOwner) { poiList ->
            points.clear()
            for (item in poiList) {
//                item.coordinate?.let { addAnnotationToMap(it.longitude(), it.latitude()) }
                Timber.i("Item name: ${item.name}; and address: ${item.address}")
                val markerId = addMarkerAndReturnId(item.coordinate!!)
                addViewAnnotation(item, markerId)
                points.add(item.coordinate!!)
            }

//                Timber.i(points.toString())
            if(points.size>1){
//                FOR CIRCLE ROUTE
//                val routeBuilder= RouteCircleGreedyBuilder()
//                routeBuilder.getRoute(7200.0, startPoint = viewModel.currentLocation ,points){
//                        result->
//                    route=result.first
//                    durationWalk=result.second
//                }
//                    #FOR_LINE_ROUTE
//    TODO Fix bug: need first initialize destination point, then add points
                if(this::destinationPoint.isInitialized){
                    val routeBuilder= RouteLineGreedy()
                    routeBuilder.getRoute(
                        1000.0,
                        viewModel.currentLocation,
                        destinationPoint,
                        points){
                            result->
                        route=result
//                        durationWalk=result
                    }
                }
                else{
                    Toast.makeText(requireContext(),"Add a destination point",Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    override fun onDestroyView() {
        mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        super.onDestroyView()
    }


//    override fun onMapLongClick(point: Point): Boolean {
//        val markerId = addMarkerAndReturnId(point)
//        addViewAnnotation(point, markerId)
//        return true
//    }


//  Markers and annotations section
    private val pointList = CopyOnWriteArrayList<Feature>()
    private var markerId = 0
    override fun onMapClick(point: Point): Boolean {
        destinationPoint=point
        addMarkerAndReturnId(destinationPoint)
//        TODO Remove marker
        mapboxMap.queryRenderedFeatures(
            RenderedQueryGeometry(mapboxMap.pixelForCoordinate(point)), RenderedQueryOptions(listOf(LAYER_ID), null)
        ) {
            onFeatureClicked(it) { feature ->
                if (feature.id() != null) {
                    viewAnnotationManager.getViewAnnotationByFeatureId(feature.id()!!)?.toggleViewVisibility()
                }
            }
        }
        return true
    }


    private fun prepareStyle(styleUri: String, bitmap: Bitmap) = style(styleUri) {
        +image(BLUE_ICON_ID) {
            bitmap(bitmap)
        }

        +geoJsonSource(SOURCE_ID) {
            featureCollection(FeatureCollection.fromFeatures(pointList))
        }

        +symbolLayer(LAYER_ID, SOURCE_ID) {
            iconImage(BLUE_ICON_ID)
            iconAnchor(IconAnchor.BOTTOM)
            iconAllowOverlap(true)
        }
    }


    private fun onFeatureClicked(
        expected: Expected<String, List<QueriedFeature>>,
        onFeatureClicked: (Feature) -> Unit
    ) {
        if (expected.isValue && expected.value?.size!! > 0) {
            expected.value?.get(0)?.feature?.let { feature ->
                onFeatureClicked.invoke(feature)
            }
        }
    }

    private fun View.toggleViewVisibility() {
        visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun addMarkerAndReturnId(point: Point): String {
        val currentId = "${MARKER_ID_PREFIX}${(markerId++)}"
        pointList.add(Feature.fromGeometry(point, null, currentId))
        val featureCollection = FeatureCollection.fromFeatures(pointList)
        mapboxMap.getStyle { style ->
            style.getSourceAs<GeoJsonSource>(SOURCE_ID)?.featureCollection(featureCollection)
        }
        return currentId
    }

    @SuppressLint("SetTextI18n")
    private fun addViewAnnotation(searchResult: SearchResult, markerId: String) {
        asyncInflater?.let {
            viewAnnotationManager.addViewAnnotation(
                resId = R.layout.annotation_view,
                options = viewAnnotationOptions {
                    geometry(searchResult.coordinate)
                    associatedFeatureId(markerId)
                    anchor(ViewAnnotationAnchor.BOTTOM)
                    allowOverlap(false)
                },
                asyncInflater = it
            ) { viewAnnotation ->
                // calculate offsetY manually taking into account icon height only because of bottom anchoring
                viewAnnotationManager.updateViewAnnotation(
                    viewAnnotation,
                    viewAnnotationOptions {
                        offsetY(markerHeight)
                    }
                )
                viewAnnotation.findViewById<TextView>(R.id.textNativeView).text =when(searchResult.name){
                    "null"->"lat=%.2f\nlon=%.2f".format(searchResult.coordinate?.latitude(), searchResult.coordinate?.longitude())
                    else-> searchResult.name
                }
            }
        }
    }

    private companion object {
        const val BLUE_ICON_ID = "blue"
        const val SOURCE_ID = "source_id"
        const val LAYER_ID = "layer_id"
        const val MARKER_ID_PREFIX = "view_annotation_"
    }





    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()
    private lateinit var routeArrowView: MapboxRouteArrowView

    private fun initNavigation() {
        mapboxNavigation = if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve()
        } else {
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(requireContext())
                    .accessToken(getString(R.string.mapbox_access_token))
                    // comment out the location engine setting block to disable simulation
                    .locationEngine(replayLocationEngine)
                    .build()
            )
        }

        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(requireContext())
            .withRouteLineBelowLayerId("road-label")
            .build()
        routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        val routeArrowOptions = RouteArrowOptions.Builder(requireContext()).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)
        mapboxNavigation.registerRoutesObserver(routesObserver)
    }


    private val routesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.routes.isNotEmpty()) {
            // generate route geometries asynchronously and render them
            val routeLines = routeUpdateResult.routes.map { RouteLine(it, null) }

            routeLineApi.setRoutes(
                routeLines
            ) { value ->
                mapboxMap.getStyle()?.apply {
                    routeLineView.renderRouteDrawData(this, value)
                }
            }

            // update the camera position to account for the new route
        } else {
            // remove the route line and route arrow from the map
            val style = mapboxMap.getStyle()
            if (style != null) {
                routeLineApi.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(
                        style,
                        value
                    )
                }
                routeArrowView.render(style, routeArrowApi.clearArrows())
            }

            // remove the route reference from camera position evaluations
        }
    }



    private fun drawRoute(points: List<Point>) {

        // execute a route request
        // it's recommended to use the
        // applyDefaultNavigationOptions and applyLanguageAndVoiceUnitOptions
        // that make sure the route request is optimized
        // to allow for support of all of the Navigation SDK features

        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(requireContext())
                .coordinatesList(points)
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .build(),
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
//                    #FOR_LINE_ROUTE
//                    durationWalk=0.0
                    durationWalk.add(0.0)
                    for(route in routes){
                        durationWalk[I]+=route.duration()
                    }
//                    setRouteAndStartNavigation(routes)
                    mapboxNavigation.setRoutes(
                        mapboxNavigation.getRoutes().plus(routes))
//                    Toast.makeText(requireContext(),
//                        "Route duration: ${durationWalk.roundToInt()/3600} hour," +
//                                " ${(durationWalk.roundToInt()/60)%60} minutes",Toast.LENGTH_LONG).show()


                    if(I==1) {

                        val inflater = layoutInflater
                        val layout = inflater.inflate(
                            R.layout.custom_toast,
                            view?.findViewById(R.id.toast_layout_root) as ViewGroup?
                        )
                        layout.findViewById<TextView>(R.id.text_duration_short).text =
                            "Fast "+getDurationWalkString(durationWalk[0])
                        layout.findViewById<TextView>(R.id.text_duration_long).text =
                            "Long "+getDurationWalkString(durationWalk[1])
                        val toast =  Toast(requireContext());
                        //                    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.duration = Toast.LENGTH_LONG;
                        toast.view = layout;
                        toast.show();
                    }
                    I++
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    // no impl
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    // no impl
                }
            }
        )
    }
    fun getDurationWalkString(duration:Double):String{
        return "route duration: ${duration.roundToInt()/3600} hour," +
                                " ${(duration.roundToInt()/60)%60} minutes"
    }
}
