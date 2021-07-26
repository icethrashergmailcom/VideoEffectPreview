package com.example.videoeffectpreview.fragments.preview

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.videoeffectpreview.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.Duration
import java.time.LocalTime
import kotlin.time.DurationUnit

/**
 * A simple [Fragment] subclass.
 * Use the [PreviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PreviewFragment : Fragment() {
    private var isExpanded: Boolean = false
    private var isRecording: Boolean = false
    private var recordingTime: LocalTime? = null

    private lateinit var previewImage: ImageView
    private lateinit var expandBtn: FloatingActionButton
    private lateinit var effectSwitch: Switch
    private lateinit var switchCameraBtn: FloatingActionButton
    private lateinit var recordBtn: FloatingActionButton
    private lateinit var timeView: TextView

    private var previewBitmapWidth: Int = 0
    private var previewBitmapHeight: Int = 0

    interface Callbacks {
        fun onPreviewIsExpandedChanged(isExpanded: Boolean)
        fun onPreviewEffectSwitched(isOn: Boolean)
        fun onPreviewCameraToggled()
        fun onPreviewStartRecording(width: Int, height: Int): Boolean // width and height is all we need for now
        fun onPreviewStopRecording(): Boolean
    }
    private var callbacks: Callbacks? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_preview, container, false)

        previewImage = view.findViewById(R.id.prevewImage)
        expandBtn = view.findViewById(R.id.expandBtn)
        effectSwitch = view.findViewById(R.id.effectSwitch)
        switchCameraBtn = view.findViewById(R.id.switchCameraBtn)
        recordBtn = view.findViewById(R.id.recordBtn)
        timeView = view.findViewById(R.id.timeView)

        setListeners()
        updateExpandBtn()

        return view
    }

    private fun setListeners() {
        expandBtn.setOnClickListener {
            if (isExpanded) collapse()
            else expand()
        }

        effectSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) switchEffectOn()
            else switchEffectOff()
        }

        switchCameraBtn.setOnClickListener {
            callbacks?.onPreviewCameraToggled()
        }

        recordBtn.setOnClickListener {
            if (isRecording) stopRecording()
            else startRecording()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    private fun updateExpandBtn() {
        val orientation = resources.configuration.orientation

        val icon: Int = when {
            isExpanded -> when (orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> R.drawable.angle_left_solid
                else -> R.drawable.angle_up_solid
            } else -> when (orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> R.drawable.angle_right_solid
                else -> R.drawable.angle_down_solid
            }
        }
        expandBtn.setImageDrawable(ContextCompat.getDrawable(context!!, icon))
    }

    private fun updateRecordBtn() {
        val icon: Int = when {
            isRecording -> R.drawable.square_solid
            else -> R.drawable.circle_solid
        }

        recordBtn.setImageDrawable(ContextCompat.getDrawable(context!!, icon))
    }

    private fun expand() {
        isExpanded = true
        updateExpandBtn()
        callbacks?.onPreviewIsExpandedChanged(isExpanded)
    }

    private fun collapse() {
        isExpanded = false
        updateExpandBtn()
        callbacks?.onPreviewIsExpandedChanged(isExpanded)
    }

    private fun switchEffectOn() {
        effectSwitch.isChecked = true
        callbacks?.onPreviewEffectSwitched(effectSwitch.isChecked)
    }

    private fun switchEffectOff() {
        effectSwitch.isChecked = false
        callbacks?.onPreviewEffectSwitched(effectSwitch.isChecked)
    }

    private fun startRecording() {
        callbacks?.onPreviewStartRecording(previewBitmapWidth, previewBitmapHeight)!!
    }

    private fun stopRecording() {
        callbacks?.onPreviewStopRecording()!!
    }

    fun onFrame(bitmap: Bitmap) {
        previewBitmapWidth = bitmap.width
        previewBitmapHeight = bitmap.height
        activity?.runOnUiThread {
            previewImage.setImageBitmap(bitmap)
            if (isRecording) {
                var s = Duration.between(recordingTime, LocalTime.now()).seconds
                timeView.text = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
            }
        }
    }

    fun onRecordingStarted() {
        isRecording = true
        updateRecordBtn()
        recordingTime = LocalTime.now()
        timeView.visibility = VISIBLE
    }

    fun onRecordingStopped(){
        isRecording = false
        updateRecordBtn()
        recordingTime = null
        timeView.visibility = GONE
    }

    fun saveState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean(IS_EXPANDED_KEY, isExpanded)
        savedInstanceState.putBoolean(IS_EFFECT_ON_KEY, effectSwitch.isChecked)
    }

    fun restoreState(savedInstanceState: Bundle?) {
        var expand = false
        var effectOn = true

        if (savedInstanceState != null) {
            expand = savedInstanceState.getBoolean(IS_EXPANDED_KEY)
            effectOn = savedInstanceState.getBoolean(IS_EFFECT_ON_KEY)
        }

        if (expand) expand()
        else collapse()

        if (effectOn) switchEffectOn()
        else switchEffectOff()
    }

    companion object {
        private const val IS_EXPANDED_KEY = "preview_isExpanded"
        private const val IS_EFFECT_ON_KEY = "preview_isEffectOn"

        @JvmStatic
        fun newInstance() {
        }
    }
}