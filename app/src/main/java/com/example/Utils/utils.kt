package com.example.Utils

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.example.medicinereminder.R

object utils {

    private var dialog: AlertDialog?=null
    fun showdialog(context: Context){
        dialog=
            AlertDialog.Builder(context).setView(R.layout.progress_dailogs).setCancelable(false).create()
        dialog!!.show()
    }
    fun hideDialog(){
        dialog?.dismiss()
    }

    fun showtoast(context: Context, message:String){
        Toast.makeText(context,message, Toast.LENGTH_SHORT).show()
    }

}