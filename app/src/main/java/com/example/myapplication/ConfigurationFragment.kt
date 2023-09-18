package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ConfigurationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ConfigurationFragment : Fragment() {
    var titleView: TextView? = null
    var listView: ListView? = null
    var state: Configuration = Configuration.AGE
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_configuration, container, false)
        listView = view?.findViewById(R.id.listView)
        titleView = view?.findViewById(R.id.listTitle)

        titleView?.text = getTitle()
        listView?.adapter = getAdapter()

        listView?.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) as CustomItem
            // Saving config
            when (state) {
                Configuration.AGE -> Configs.conf[Configuration.AGE] = selectedItem.title
                Configuration.NATIVE_LANG -> Configs.conf[Configuration.NATIVE_LANG] = selectedItem.title
                Configuration.ENGLISH_LEVEL -> Configs.conf[Configuration.ENGLISH_LEVEL] =
                    selectedItem.title
                Configuration.GOAL -> Configs.conf[Configuration.GOAL] = selectedItem.title
            }
            // Changing state
            when (state) {
                Configuration.AGE -> state = Configuration.NATIVE_LANG
                Configuration.NATIVE_LANG -> state = Configuration.ENGLISH_LEVEL
                Configuration.ENGLISH_LEVEL -> state = Configuration.GOAL
                Configuration.GOAL -> transitionToLoadingChat()
            }
            titleView?.text = getTitle()
            listView?.adapter = getAdapter()
        }

        return view
    }

    fun getTitle(): String {
        return when (state) {
            Configuration.AGE -> requireContext().resources.getString(R.string.hello_phrase)
            Configuration.NATIVE_LANG -> requireContext().resources.getString(R.string.native_lang_phrase)
            Configuration.ENGLISH_LEVEL -> requireContext().resources.getString(R.string.english_level_phrase)
            Configuration.GOAL -> requireContext().resources.getString(R.string.goal_phrase)
        }
    }

    fun getAdapter(): ListAdapter {
        when (state) {
            Configuration.AGE -> return CustomListAdapter(
                requireContext(),
                mutableListOf(
                    CustomItem("Under 16"),
                    CustomItem("17 - 24"),
                    CustomItem("25 - 34"),
                    CustomItem("35 - 44"),
                    CustomItem("45 - 54"),
                    CustomItem("55 or older ")
                )
            )
            Configuration.NATIVE_LANG -> return CustomListAdapter(
                requireContext(),
                mutableListOf(
                    CustomItem("Kyrgyz (Кыргызча)"),
                    CustomItem("Russian (Русский)"),
                    CustomItem("Kazakh (Қазақ)"),
                    CustomItem("Uzbek (Оʻzbek)"),
                    CustomItem("Korean (한국어)"),
                    CustomItem("Chinese"),
                    CustomItem("Japan (日本語)"),
                    CustomItem("Indian")
                )
            )
            Configuration.ENGLISH_LEVEL -> return CustomListAdapter(
                requireContext(),
                mutableListOf(
                    CustomItem("A1 - Beginner"),
                    CustomItem("A2 - Elementary"),
                    CustomItem("B1 - Intermediate"),
                    CustomItem("B2 - Upper-Intermediate"),
                    CustomItem("C1 - Advanced")
                )
            )
            Configuration.GOAL -> return CustomListAdapter(
                requireContext(),
                mutableListOf(
                    CustomItem("Self Improvement"),
                    CustomItem("Travel"),
                    CustomItem("Education"),
                    CustomItem("Work")
                )
            )
        }
    }

    fun transitionToLoadingChat() {
        val mainFragment = LoadingFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, mainFragment)
            .commit()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ConfigurationFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ConfigurationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

enum class Configuration {
    AGE, NATIVE_LANG, ENGLISH_LEVEL, GOAL
}

object Configs {
    val conf = mutableMapOf<Configuration, String>()
}

enum class RemoteConfiguration {
    OPEN_AI, SPEECH_AI
}
object RemoteConfigs {
    val conf = mutableMapOf<RemoteConfiguration, String>()
}