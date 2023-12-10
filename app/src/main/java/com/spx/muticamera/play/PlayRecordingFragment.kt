package com.spx.muticamera.play

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.spx.muticamera.R

class PlayRecordingFragment : Fragment() {

    private val TAG = "tmf_PlayRecordingFragment"
    private var player: SimpleExoPlayer? = null
    private lateinit var playView: PlayerView;
    private lateinit var backView: View;
    private lateinit var recordFilePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordFilePath = arguments?.getString("recording")!!
        Log.i(TAG, "onCreate: recordFilePath:${recordFilePath}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_play_recording, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backView = view.findViewById(R.id.iv_back)
        playView = view.findViewById(R.id.video_view)
        playView.layoutParams.let {
            it.width = resources.displayMetrics.widthPixels
            it.height = it.width * 16 / 9
            playView.layoutParams = it
        }
        initializePlayer();

        backView.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun initializePlayer() {
        player = SimpleExoPlayer.Builder(requireContext())
            .build()
            .also { exoPlayer ->
                playView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(recordFilePath)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playWhenReady = true
                exoPlayer.prepare()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
    }
}