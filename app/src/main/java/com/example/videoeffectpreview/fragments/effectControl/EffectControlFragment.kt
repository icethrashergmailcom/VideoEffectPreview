package com.example.videoeffectpreview.fragments.effectControl

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.videoeffectpreview.R
import com.example.videoeffectpreview.controls.PadControl
import com.example.videoeffectpreview.fragments.preview.PreviewFragment

/**
 * A simple [Fragment] subclass.
 * Use the [EffectControlFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EffectControlFragment : Fragment() {
    private var padPositionX = 0.0
    private var padPositionY = 0.0

    private lateinit var padControl: PadControl

    interface Callbacks {
        fun onPadPositionChanged(x: Double, y: Double)
    }
    private var callbacks: Callbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_effect_control, container, false)

        padControl = view.findViewById<PadControl>(R.id.padControl)
        padControl.setCallbacks(object : PadControl.Callbacks {
            override fun onPadPositionChanged(x: Float, y: Float) {
                padPositionX = x.toDouble()
                padPositionY = y.toDouble()
                callbacks?.onPadPositionChanged(padPositionX, padPositionY)
            }
        })

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    fun saveState(savedInstanceState: Bundle) {
        savedInstanceState.putDouble(EffectControlFragment.X_KEY, padPositionX)
        savedInstanceState.putDouble(EffectControlFragment.Y_KEY, padPositionY)
    }

    fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            padPositionX = savedInstanceState.getDouble(EffectControlFragment.X_KEY)
            padPositionY = savedInstanceState.getDouble(EffectControlFragment.Y_KEY)

            padControl.setPosition(padPositionX.toFloat(), padPositionY.toFloat())
        }
    }

    companion object {
        private const val X_KEY = "effect_config_x"
        private const val Y_KEY = "effect_config_y"

        @JvmStatic
        fun newInstance() {
        }
    }
}