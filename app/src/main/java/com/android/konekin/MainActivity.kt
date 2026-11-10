package com.android.konekin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.konekin.databinding.ActivityMainBinding
import com.android.konekin.model.Data
import com.android.konekin.model.Users
import com.android.konekin.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // simpan data lengkap (bukan cuma nama) supaya id bisa diambil saat item diklik
    private val userList = ArrayList<Data>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // beri jarak dari status bar / navigation bar (edge-to-edge)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // klik item -> buka UserDetailActivity sambil bawa id user yang diklik
        binding.lvNama.setOnItemClickListener { _, _, position, _ ->
            val user = userList[position]
            val intent = Intent(this, UserDetailActivity::class.java)
            intent.putExtra(UserDetailActivity.EXTRA_ID, user.id)
            startActivity(intent)
        }

        getUsers()
    }

    private fun getUsers() {
        binding.progressBar.visibility = View.VISIBLE

        val client = ApiClient.getInstance()
        val response = client.getAllUsers()

        response.enqueue(object : Callback<Users> {
            override fun onResponse(call: Call<Users>, response: Response<Users>) {
                binding.progressBar.visibility = View.GONE
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    // contoh: 429 Too Many Requests dari API dummy
                    Toast.makeText(
                        this@MainActivity,
                        "Gagal ambil data (kode ${response.code()})",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                userList.clear()
                userList.addAll(body.data)

                val names = userList.map { it.employeeName }
                val listAdapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_list_item_1,
                    names
                )
                binding.lvNama.adapter = listAdapter
            }

            override fun onFailure(call: Call<Users>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Koneksi error", Toast.LENGTH_LONG).show()
            }
        })
    }
}
