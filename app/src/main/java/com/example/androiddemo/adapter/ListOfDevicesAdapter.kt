package com.example.androiddemo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.androiddemo.R
import com.example.androiddemo.interfaces.OnClickConnect

class ListOfDevicesAdapter(var context : Context,var listofDevices : ArrayList<String>, var onClickConnect: OnClickConnect) : RecyclerView.Adapter<ListOfDevicesAdapter.ViewHolder>() {

    var connected : Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        /* Setting up the layout with the adapter view */
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_of_devices, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        /* Assigining the count of devices */
        return listofDevices.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.macAdress.setText(listofDevices.get(position))
        holder.coonnectDisconnect.setOnClickListener {
            if (!connected){
                holder.coonnectDisconnect.setText("Disconnect")
                connected = true
            }
            else{
                holder.coonnectDisconnect.setText("Connect")
                connected = false
            }
            onClickConnect.onClickConnectOrDisconnect()
        }
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {

        /* Initializing the view with the text view variables */
        val macAdress : TextView = itemView.findViewById(R.id.mac_TV)
        val coonnectDisconnect : TextView = itemView.findViewById<TextView?>(R.id.connect_disconnect_TV)

    }
}