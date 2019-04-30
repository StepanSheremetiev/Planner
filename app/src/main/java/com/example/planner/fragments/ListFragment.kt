package com.example.planner.fragments

import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.view.View
import com.arellomobile.mvp.MvpAppCompatFragment
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.example.planner.FRAGMENT_TAG_ADDTASK
import com.example.planner.R
import com.example.planner.adapter.TaskArrayAdapter
import com.example.planner.presenters.MainPresenter
import com.example.planner.task.Task
import com.example.planner.viewer.MainView

const val ADAPTER_POSITION_ALL = "adapterPositionAll"
const val ADAPTER_POSITION_FAV = "adapterPositionFav"

abstract class ListFragment : MvpAppCompatFragment(), MainView {
    @InjectPresenter
    lateinit var presenter: MainPresenter
    private lateinit var adapterList: TaskArrayAdapter

    @ProvidePresenter
    fun provideMainPresenter() = MainPresenter(requireContext(), LoaderManager.getInstance(this))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
    }

    abstract override fun showProgressBars()

    override fun onListUpdate(tasks: Map<Int, Task>) {
        hideProgressBars()
        adapterList.setList(getList(tasks))
    }

    override fun editSelectedTask(task: Task?) {
        var fragmentAdd = requireActivity().supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_ADDTASK)
        val fragmentArguments = bundlePutPosition(task, adapterList.getSelectedPosition())

        if (fragmentAdd == null || !fragmentAdd.isAdded) {
            fragmentAdd = AddTaskFragment()
            fragmentAdd.arguments = fragmentArguments

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.edit_fragment, fragmentAdd, FRAGMENT_TAG_ADDTASK)
                .commit()
        } else {
            fragmentAdd as AddTaskFragment
            fragmentAdd.arguments = fragmentArguments
            fragmentAdd.setTask()
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.updateFields(requireContext(), LoaderManager.getInstance(this))
        presenter.onStart()
        presenter.getTasksList()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    private fun initAdapter() {
        adapterList = TaskArrayAdapter(requireContext(), presenter)
        checkSavedPosition(this.arguments)

        this.arguments?.getInt(ADAPTER_POSITION_ALL)?.let {
            presenter.updateAdapterPosition(it)
        }

        init(adapterList)
    }

    override fun setAdapterSelectedPosition(position: Int) {
        adapterList.setSelectedPosition(position)
    }

    override fun setAdapterStartPosition() {
        adapterList.setSelectedStartedPosition()
        this.arguments?.remove(ADAPTER_POSITION_ALL)
    }

    abstract fun checkSavedPosition(bundle: Bundle?)
    abstract fun bundlePutPosition(task: Task?, position: Int): Bundle
    abstract fun init(adapter: TaskArrayAdapter)
    abstract fun hideProgressBars()
    abstract fun getList(tasks: Map<Int, Task>): List<Task>
}