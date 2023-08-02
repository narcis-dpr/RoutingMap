package com.example.maprouting.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.maprouting.R
import com.example.maprouting.databinding.BottomSheetRouteInfoBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RouteBottomSheet(context: Context) : BottomSheetDialogFragment() {

    private var binding: BottomSheetRouteInfoBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_route_info, container, false)
        binding = BottomSheetRouteInfoBinding.bind(view)
        binding!!.infoRout.setText(R.string.route_detect)

        binding!!.btnBack.setOnClickListener {
            val intent = Intent(context, TiltCameraActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}
