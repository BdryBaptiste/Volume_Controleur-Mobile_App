package com.example.volumecontroller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.volumecontroller.Adapter.ApplicationAdapter
import com.example.volumecontroller.databinding.LayoutPageBinding

class PageFragment : Fragment() {

    private var _binding: LayoutPageBinding? = null
    private val binding get() = _binding!!

    private lateinit var applications: List<String>
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applications = arguments?.getStringArrayList(ARG_APPLICATIONS)?.toList() ?: emptyList()
        apiService = (activity as MainActivity).apiService
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ApplicationAdapter(apiService)
        adapter.setApplications(applications)
        val layoutManager = GridLayoutManager(context, 5)
        layoutManager.orientation = GridLayoutManager.VERTICAL
        layoutManager.isItemPrefetchEnabled = true

        // Centrer les éléments dans le RecyclerView
        binding.applicationsRecyclerView.layoutManager = layoutManager
        binding.applicationsRecyclerView.adapter = adapter
        binding.applicationsRecyclerView.isNestedScrollingEnabled = false
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_APPLICATIONS = "applications"

        fun newInstance(applications: List<String>): PageFragment {
            val fragment = PageFragment()
            fragment.arguments = bundleOf(ARG_APPLICATIONS to ArrayList(applications))
            return fragment
        }
    }
}
