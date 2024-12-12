package com.example.medicinereminder

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MedicineStore : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var cartBadge: TextView
    private lateinit var searchBar: EditText

    private val products = listOf(
        Product("Paracetamol - 10 Tablets", 50.0, R.drawable.ibs_medicine),
        Product("Ibuprofen - 6 Tablets", 60.0, R.drawable.antacid),
        Product("Cough Syrup - 100ml", 100.0, R.drawable.spray),
        Product("Vitamin D - 30 Capsules", 200.0, R.drawable.vocveda),
        Product("Antibiotic - 12 Tablets", 150.0, R.drawable.b_capsules),
        Product("Antacid - 20 Tablets", 30.0, R.drawable.paracip),
        Product("Pain Relief Spray - 50g", 250.0, R.drawable.breath_acid),
        Product("Diabetes Tablet - 20 tab", 120.0, R.drawable.s_cal),
        Product("Blood Pressure Med", 80.0, R.drawable.okacet_cold),
        Product("Eye Drops - 15ml", 90.0, R.drawable.omeo_allergy)
    )


    private val cart = mutableListOf<Product>()

    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicine_store)

        recyclerView = findViewById(R.id.medicineGrid)
        cartBadge = findViewById(R.id.cartBadge)
        searchBar = findViewById(R.id.searchBar)

        // Initialize the adapter with the full product list
        productAdapter = ProductAdapter(products, ::addToCart, ::removeFromCart)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = productAdapter

        val divider = GridDividerItemDecoration(
            this,
            color = getColor(R.color.light_grey), // Adjust color as needed
            thickness = 1f // Thin lines
        )
        recyclerView.addItemDecoration(divider)


        findViewById<FrameLayout>(R.id.cart_layout).setOnClickListener {
            // Launch Cart Activity
            val intent = Intent(this, Cart::class.java)
            intent.putParcelableArrayListExtra("cart", ArrayList(cart))
            startActivity(intent)

        }
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterProducts(query: String) {
        val filteredList = products.filter { it.name.contains(query, ignoreCase = true) }
        productAdapter.updateList(filteredList)
    }

    private fun addToCart(product: Product) {
        val existingProduct = cart.find { it.name == product.name }
        if (existingProduct != null) {
            existingProduct.quantity++
        } else {
            product.quantity = 1
            cart.add(product)
        }
        updateBadge()
    }

    private fun removeFromCart(product: Product) {
        val existingProduct = cart.find { it.name == product.name }
        if (existingProduct != null && existingProduct.quantity > 1) {
            existingProduct.quantity--
        } else {
            cart.remove(product)
        }
        updateBadge()
    }


    private fun updateBadge() {
        cartBadge.text = cart.size.toString()
        cartBadge.visibility = if (cart.isNotEmpty()) View.VISIBLE else View.GONE
    }
}
