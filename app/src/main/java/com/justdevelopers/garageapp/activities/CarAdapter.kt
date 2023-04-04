package com.justdevelopers.garageapp.activities

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.justdevelopers.garageapp.databinding.CarsRowBinding

class CarAdapter(private val items:ArrayList<CarEntity>,
                private val addImageListener:(id:Int)->Unit,
                private val deleteListener:(id:Int)->Unit
    ):Adapter<CarAdapter.MainHolder>() {
        inner class MainHolder(carsView: CarsRowBinding):RecyclerView.ViewHolder(carsView.root){
            val llMain = carsView.llMain
            val tvModel=carsView.tvModel
            val tvCompany = carsView.tvCompany
            val btnAddImage= carsView.btnAddImage
            val btnDelete = carsView.btnDelete
            val ivImage = carsView.ivImage
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainHolder {
        return MainHolder(CarsRowBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: MainHolder, position: Int) {
        val context = holder.itemView.context
        val item = items[position]
        holder.tvModel.text = item.model
        holder.tvCompany.text = item.company
        if(item.image.isNotEmpty()){
            holder.ivImage.setImageURI(Uri.parse(item.image))
        }
        holder.btnDelete.setOnClickListener{
            deleteListener.invoke(item.id)
        }
        holder.btnAddImage.setOnClickListener {
            addImageListener.invoke(item.id)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}