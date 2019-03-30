package com.example.planner.adapter

import android.content.Context
import android.graphics.Paint
import android.support.v7.util.DiffUtil
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.planner.R
import com.example.planner.enums.TaskActionId
import com.example.planner.presenters.IMainPresenter
import com.example.planner.task.Task

const val ID_ELEMENT = 0
const val ID_HEADER = 1
const val TITLE_FAVORITE = "Favorites:"
const val TITLE_OTHERS = "Others:"

class TaskArrayAdapter(
    private val context: Context,
    private val presenter: IMainPresenter
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var taskList: MutableList<Task> = mutableListOf()
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var posHeadOther = 0


    override fun getItemCount(): Int {
        posHeadOther = taskList.indexOfFirst { !it.favorite } + 1

        return if (taskList.isNotEmpty()) {
            if (taskList[0].favorite && posHeadOther > 0) {
                taskList.size + 2
            } else {
                taskList.size
            }
        } else {
            taskList.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        posHeadOther = taskList.indexOfFirst { !it.favorite } + 1

        return if ((posHeadOther > 0 && taskList[0].favorite) && (position == 0 || position == posHeadOther)) {
            ID_HEADER
        } else {
            ID_ELEMENT
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == ID_HEADER) {
            val vh = holder as ViewHolderHeader

            if (position == 0) {
                vh.headerTextView?.text = TITLE_FAVORITE
            } else {
                vh.headerTextView?.text = TITLE_OTHERS
            }
        } else {
            val vh = holder as ViewHolder
            posHeadOther = taskList.indexOfFirst { !it.favorite } + 1

            var offset = if (posHeadOther > 0 && taskList[0].favorite) 1 else 0

            if (posHeadOther in 1..vh.adapterPosition && taskList[0].favorite) offset = 2

            vh.titleTextView?.text = taskList[vh.adapterPosition - offset].title
            vh.descriptionTextView?.text = taskList[vh.adapterPosition - offset].description

            if (taskList[vh.adapterPosition - offset].done) {
                vh.titleTextView?.paintFlags = ((vh.titleTextView?.paintFlags ?: 0)
                        or (Paint.STRIKE_THRU_TEXT_FLAG))
                vh.descriptionTextView?.paintFlags = ((vh.descriptionTextView?.paintFlags ?: 0)
                        or (Paint.STRIKE_THRU_TEXT_FLAG))
            } else {
                vh.titleTextView?.paintFlags = 0
                vh.descriptionTextView?.paintFlags = 0
            }

            vh.moreImageView?.setOnClickListener {
                showPopup(context, it, taskList[vh.adapterPosition - offset])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ID_HEADER) {
            ViewHolderHeader(inflater.inflate(R.layout.listview_header, parent, false))
        } else {
            ViewHolder(inflater.inflate(R.layout.list_item_task, parent, false))
        }
    }

    fun setList(newList: List<Task>) {
        val diffResult = DiffUtil.calculateDiff(TaskUpdateListDiffUtilCallback(taskList, newList), true)
        taskList.clear()
        taskList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private fun showPopup(context: Context, view: View, task: Task) {
        val popup: PopupMenu?
        popup = PopupMenu(context, view)
        popup.inflate(R.menu.task)
        val favItem = popup.menu.findItem(R.id.favoriteTaskButton)
        val doneItem = popup.menu.findItem(R.id.doneTaskButton)
        favItem.title = if (task.favorite) context.resources.getString(R.string.taskMenuRemoveFavorite)
        else context.resources.getString(R.string.taskMenuAddFavorite)

        doneItem.title = if (task.done) context.resources.getString(R.string.taskMenuUndone)
        else context.resources.getString(R.string.taskMenuDone)

        popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.editTaskButton -> {
                    presenter.editTask(task)
                }
                R.id.removeTaskButton -> {
                    presenter.updateTask(TaskActionId.ACTION_REMOVE.getId(), task)
                }
                R.id.favoriteTaskButton -> {
                    task.favorite = !task.favorite
                    presenter.updateTask(TaskActionId.ACTION_FAVORITE.getId(), task)
                }
                R.id.doneTaskButton -> {
                    task.done = !task.done
                    presenter.updateTask(TaskActionId.ACTION_DONE.getId(), task)
                }
            }
            true
        })

        popup.show()
    }

    private class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var titleTextView: TextView? = view.findViewById(R.id.listTaskTitle)
        var descriptionTextView: TextView? = view.findViewById(R.id.listTaskDescription)
        var moreImageView: Button? = view.findViewById(R.id.listMoreButton)
    }

    private class ViewHolderHeader(view: View) : RecyclerView.ViewHolder(view) {
        var headerTextView: TextView? = view.findViewById(R.id.listHeader)
    }
}