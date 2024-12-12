package com.example.medicinereminder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(
    private var products: List<Product>,
    private val onAddToCart: (Product) -> Unit,
    private val onRemoveFromCart: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {


    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.product_name)
        val price: TextView = view.findViewById(R.id.product_price)
        val productImage: ImageView = view.findViewById(R.id.product_image)
        val addButton: Button = view.findViewById(R.id.btn_add)
        val removeButton: Button = view.findViewById(R.id.btn_remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.name.text = product.name
        holder.price.text = "₹${product.price}"

        holder.productImage.setImageResource(product.imageResId)

        holder.addButton.visibility = View.VISIBLE
        holder.removeButton.visibility = View.GONE

        holder.addButton.setOnClickListener { onAddToCart(product)
            holder.addButton.visibility = View.GONE  // Hide "Add to Cart"
            holder.removeButton.visibility = View.VISIBLE }

        holder.removeButton.setOnClickListener { onRemoveFromCart(product)
            holder.addButton.visibility = View.VISIBLE  // Show "Add to Cart"
            holder.removeButton.visibility = View.GONE
        }
    }

    fun updateList(newList: List<Product>) {
        products = newList
        notifyDataSetChanged()
    }

    override fun getItemCount() = products.size
}
