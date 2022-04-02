package com.santo.wikiguide.UI.map

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
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
import com.mapbox.turf.TurfMeasurement.destination
import com.santo.wikiguide.R
import com.santo.wikiguide.databinding.FragmentMapBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList


@MapboxExperimental
@AndroidEntryPoint
class MapFragment : Fragment(),OnMapClickListener  {

    private lateinit var binding: FragmentMapBinding
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var viewAnnotationManager: ViewAnnotationManager


    private lateinit var mapboxNavigation: MapboxNavigation
    private val replayLocationEngine = ReplayLocationEngine(MapboxReplayer())


    private val viewModel: MapFragmentViewModel by viewModels()


    private var markerWidth = 0
    private var markerHeight = 0
    private val asyncInflater by lazy { context?.let { AsyncLayoutInflater(it) } }




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
        mapboxNavigation.registerRoutesObserver(routesObserver)
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(requireContext())
            .withRouteLineBelowLayerId("road-label")
            .build()
        routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        val routeArrowOptions = RouteArrowOptions.Builder(requireContext()).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)

        initMap()

        binding.inputCategoryName.setText("cafe")
        binding.inputCategoryLimit.setText("5")


        binding.addButton.setOnClickListener {
            viewModel.getPOIs(binding.inputCategoryName.text.toString(),binding.inputCategoryLimit.text.toString().toInt())
            val points:ArrayList<Point> =ArrayList<Point>()
            viewModel.poiList.observe(viewLifecycleOwner, Observer {
                    poiList->
                for(item in poiList){
//                item.coordinate?.let { addAnnotationToMap(it.longitude(), it.latitude()) }
                    Timber.i("Item name: ${item.name}; and address: ${item.address}")
                    val markerId = addMarkerAndReturnId(item.coordinate!!)
                    addViewAnnotation(item, markerId)
                    points.add(item.coordinate!!)
                }
            })
            mapView.gestures.addOnMapLongClickListener{
                findRoute(points)
                true
            }
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

        val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
            mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
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
    }



//    override fun onMapLongClick(point: Point): Boolean {
//        val markerId = addMarkerAndReturnId(point)
//        addViewAnnotation(point, markerId)
//        return true
//    }

    override fun onMapClick(point: Point): Boolean {
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



    private val pointList = CopyOnWriteArrayList<Feature>()
    private var markerId = 0
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



    private fun findRoute(points: List<Point>) {

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
//                .coordinates(points.toString())
//                .coordinates(points[0],null,points[1])
                // provide the bearing for the origin of the request to ensure
                // that the returned route faces in the direction of the current user movement
//                .bearingsList(
//                    listOf(
//                        Bearing.builder()
//                            .angle(originLocation.bearing.toDouble())
//                            .degrees(45.0)
//                            .build(),
//                        null
//                    )
//                )
//                .layersList(listOf(mapboxNavigation.getZLevel(), null))
//                .layersList(listOf(points.size, null))
//                .alternatives(true)
                .build(),
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
//                    setRouteAndStartNavigation(routes)
                    mapboxNavigation.setRoutes(routes)
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
}
