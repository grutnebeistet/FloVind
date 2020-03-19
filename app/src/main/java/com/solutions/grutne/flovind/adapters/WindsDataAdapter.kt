package com.solutions.grutne.flovind.adapters

import android.content.Context
import android.database.Cursor
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.solutions.grutne.flovind.R
import com.solutions.grutne.flovind.ForecastFragment


class WindsDataAdapter(private val mContext: Context) : RecyclerView.Adapter<WindsDataAdapter.WindsViewHolder>() {
    private var mCursor: Cursor? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WindsViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.wind_list_item, parent, false)
        return WindsViewHolder(view)
    }

    override fun onBindViewHolder(holder: WindsViewHolder, position: Int) {
        mCursor!!.moveToPosition(position)
        val time = mCursor!!.getString(ForecastFragment.INDEX_WIND_TIME)
        val speed = mCursor!!.getString(ForecastFragment.INDEX_WIND_SPEED)
        val winDir = mCursor!!.getString(ForecastFragment.INDEX_WIND_DIR)

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

        holder.mSpeed.text = "$speed m/s"
        holder.mTime.text = time
        holder.mWinDirImg.setImageResource(dirImg)
    }

    fun swapCursor(newCursor: Cursor?) {
        mCursor = newCursor
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (null == mCursor) 0 else mCursor!!.count
    }

    inner class WindsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var mSpeed: TextView = view.findViewById(R.id.wind_speed)
        var mTime: TextView = view.findViewById(R.id.wind_time)
        var mWinDirImg: ImageView = view.findViewById(R.id.wind_dir_img)

    }
}
