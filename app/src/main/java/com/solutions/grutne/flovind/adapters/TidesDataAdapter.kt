package com.solutions.grutne.flovind.adapters

import android.content.Context
import android.database.Cursor
import android.media.Image
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.solutions.grutne.flovind.R
import com.solutions.grutne.flovind.TidesFragment

import kotlinx.android.synthetic.main.tide_list_item.view.*
import timber.log.Timber
import java.util.*

class TidesDataAdapter(private var mContext: Context) : RecyclerView.Adapter<TidesDataAdapter.TdViewHolder>() {
    private var mCursor: Cursor? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TdViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.tide_list_item, parent, false)
        return TdViewHolder(view)
    }

    override fun onBindViewHolder(holder: TdViewHolder, position: Int) {
        mCursor!!.moveToPosition(position)

        var flag = mCursor!!.getString(TidesFragment.INDEX_FLAG)
        val time = mCursor!!.getString(TidesFragment.INDEX_LEVEL_TIME)
        val level = mContext.getString(R.string.level_format, mCursor!!.getString(TidesFragment.INDEX_TIDE_LEVEL))

        if (position % 2 != 0) {
            holder.mDivider.visibility = View.INVISIBLE
        }
        if (flag == "high")
            holder.mFlagImg.setImageResource(R.drawable.high_tide)
        else if (flag == "low")
            holder.mFlagImg.setImageResource(R.drawable.low_tide)

        flag = if (Locale.getDefault().displayLanguage == "norsk bokmål")
            if (flag == "high")
                "Høyvann"
            else "Lavvann"
        else mContext.getString(R.string.flag_format, flag)

        holder.mFlag.text = flag
        holder.mTime.text = time
        holder.mLevel.text = level
    }

    fun swapCursor(newCursor: Cursor?) {
        mCursor = newCursor
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (null == mCursor) 0 else mCursor!!.count
    }

    inner class TdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var mFlag: TextView = view.findViewById(R.id.tide_item_flag)
        var mTime: TextView = view.findViewById(R.id.tide_item_time)
        var mLevel: TextView = view.findViewById(R.id.tide_item_value)
        var mDivider: View = view.findViewById(R.id.divider_list_item)
        var mFlagImg: ImageView = view.findViewById(R.id.tide_item_flag_img)
    }
}
