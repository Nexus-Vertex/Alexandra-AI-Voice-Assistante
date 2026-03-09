package com.example.myapp

import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class IntroAdapter(private val slides: List<Slide>) :
    RecyclerView.Adapter<IntroAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgSlide)
        val titleTop: TextView = v.findViewById(R.id.txtTitleTop)
        val descTop: TextView = v.findViewById(R.id.txtDescTop)
        val titleBottom: TextView = v.findViewById(R.id.txtTitleBottom)
        val descBottom: TextView = v.findViewById(R.id.txtDescBottom)
        val logoArea: LinearLayout = v.findViewById(R.id.logoArea)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.slide_item, parent, false)
        )
    }

    override fun getItemCount() = slides.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val slide = slides[position]
        holder.img.setImageResource(slide.image)
        holder.titleTop.text = slide.title
        holder.descTop.text = slide.desc

        if (position == 0) {
            // Slide 1 — cacher titre/desc en bas, montrer logo Alexandra
            holder.titleBottom.visibility = View.GONE
            holder.descBottom.visibility = View.GONE
            holder.logoArea.visibility = View.VISIBLE
        } else {
            // Slides 2 et 3 — montrer titre/desc en bas, cacher logo
            holder.titleBottom.visibility = View.VISIBLE
            holder.descBottom.visibility = View.VISIBLE
            holder.titleBottom.text = slide.title
            holder.descBottom.text = slide.desc
            holder.logoArea.visibility = View.GONE
        }
    }
}