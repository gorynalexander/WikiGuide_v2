package com.santo.wikiguide.UI.map

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.mapbox.android.gestures.MoveGestureDetector
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
import com.mapbox.search.result.SearchResult
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
        initMap()
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

        viewModel.getPOIs("cafe",10)
        viewModel.poiList.observe(viewLifecycleOwner, Observer {
            poiList->
            for(item in poiList){
//                item.coordinate?.let { addAnnotationToMap(it.longitude(), it.latitude()) }
                Timber.i("Item name: ${item.name}; and address: ${item.address}")
                val markerId = addMarkerAndReturnId(item.coordinate!!)
                addViewAnnotation(item, markerId)
            }
        })

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

    private fun Float.dpToPx() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        this@MapFragment.resources.displayMetrics
    )

    private companion object {
        const val BLUE_ICON_ID = "blue"
        const val SOURCE_ID = "source_id"
        const val LAYER_ID = "layer_id"
        const val MARKER_ID_PREFIX = "view_annotation_"
    }








//    private fun addAnnotationToMap(longitude:Double, latitude:Double) {
//// Create an instance of the Annotation API and get the PointAnnotationManager.
//        bitmapFromDrawableRes(
//            this@MapFragment.requireContext(),
//            R.drawable.red_marker
//        )?.let {
//            val annotationApi = mapView.annotations
//            val pointAnnotationManager = annotationApi.createPointAnnotationManager()
//// Set options for the resulting symbol layer.
//            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
//// Define a geographic coordinate.
//                .withPoint(Point.fromLngLat(longitude, latitude))
//                .withDraggable(true)
//// Specify the bitmap you assigned to the point annotation
//// The bitmap will be added to map style automatically.
//                .withIconImage(it)
//// Add the resulting pointAnnotation to the map.
//            pointAnnotationManager.create(pointAnnotationOptions)
//        }
//    }
//    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
//        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))
//
//    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
//        if (sourceDrawable == null) {
//            return null
//        }
//        return if (sourceDrawable is BitmapDrawable) {
//            sourceDrawable.bitmap
//        } else {
//// copying drawable object to not manipulate on the same reference
//            val constantState = sourceDrawable.constantState ?: return null
//            val drawable = constantState.newDrawable().mutate()
//            val bitmap: Bitmap = Bitmap.createBitmap(
//                drawable.intrinsicWidth, drawable.intrinsicHeight,
//                Bitmap.Config.ARGB_8888
//            )
//            val canvas = Canvas(bitmap)
//            drawable.setBounds(0, 0, canvas.width, canvas.height)
//            drawable.draw(canvas)
//            bitmap
//        }
//    }

}
