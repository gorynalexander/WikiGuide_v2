package com.santo.wikiguide.UI.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.loader.MapboxMaps
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.search.MapboxSearchSdk
import com.santo.wikiguide.R
import com.santo.wikiguide.databinding.FragmentMapBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


@AndroidEntryPoint
class MapFragment : Fragment() {

    private lateinit var mapView: MapView

    private lateinit var binding: FragmentMapBinding

    private val viewModel: MapFragmentViewModel by viewModels()

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
        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    mapView.location.updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }
                }
            }
        )



        mapView.getMapboxMap().addOnMapLongClickListener{
                point ->
//          Add UI
            viewModel.getReverseGeocodingResult(point)
            addAnnotationToMap(point.longitude(),point.latitude())
            true
        }


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

        addAnnotationToMap(30.748458862304688, 46.44748306274414)
        addAnnotationToMap(30.848458862304688, 46.55748306274414)
        viewModel.getPOIs("cafe",10)
        viewModel.poiList.observe(viewLifecycleOwner, Observer {
            poiList->
            for(item in poiList){
                item.coordinate?.let { addAnnotationToMap(it.longitude(), it.latitude()) }
                Timber.i("Item name: ${item.name}; and address: ${item.address}")
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


    private fun addAnnotationToMap(longitude:Double, latitude:Double) {
// Create an instance of the Annotation API and get the PointAnnotationManager.
        bitmapFromDrawableRes(
            this@MapFragment.requireContext(),
            R.drawable.red_marker
        )?.let {
            val annotationApi = mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager()
// Set options for the resulting symbol layer.
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
// Define a geographic coordinate.
                .withPoint(Point.fromLngLat(longitude, latitude))
                .withDraggable(true)
// Specify the bitmap you assigned to the point annotation
// The bitmap will be added to map style automatically.
                .withIconImage(it)
// Add the resulting pointAnnotation to the map.
            pointAnnotationManager.create(pointAnnotationOptions)
        }
    }
    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
// copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

}
