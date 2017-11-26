package com.solutions.grutne.flovind.adapters

import android.content.Context
import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.solutions.grutne.flovind.R
import com.solutions.grutne.flovind.TidesFragment


import kotlinx.android.synthetic.main.wind_list_item.view.*


class WindsDataAdapter(internal var mContext: Context) : RecyclerView.Adapter<WindsDataAdapter.TdViewHolder>() {
    internal var mCursor: Cursor? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TdViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.wind_list_item, parent, false)
        return TdViewHolder(view)
    }

    override fun onBindViewHolder(holder: TdViewHolder, position: Int) {
        mCursor!!.moveToPosition(position)
        val time = mCursor!!.getString(TidesFragment.INDEX_WIND_TIME)
        val speed = mCursor!!.getString(TidesFragment.INDEX_WIND_SPEED)
        val winDir = mCursor!!.getString(TidesFragment.INDEX_WIND_DIR)

        var dirImg = 0
        when (winDir) {
            "N" -> dirImg = R.drawable.north
            "E" -> dirImg = R.drawable.east
            "S" -> dirImg = R.drawable.south
            "W" -> dirImg = R.drawable.west
            "NE" -> dirImg = R.drawable.north_east
            "NW" -> dirImg = R.drawable.north_west
            "SE" -> dirImg = R.drawable.south_east
            "SW" -> dirImg = R.drawable.south_west
        }

        holder.mSpeed!!.text = speed + " m/s"
        holder.mTime!!.text = time
        holder.mWinDirImg!!.setImageResource(dirImg)
    }

    fun swapCursor(newCursor: Cursor?) {
        mCursor = newCursor
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (null == mCursor) 0 else mCursor!!.count
    }

     inner class TdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var mSpeed = view.findViewById<TextView>(R.id.wind_speed)
        var mTime = view.findViewById<TextView>(R.id.wind_time)
        var mWinDirImg = view.findViewById<ImageView>(R.id.wind_dir_img)

        init {
          //  ButterKnife.bind(this, view)
        }
    }
}
