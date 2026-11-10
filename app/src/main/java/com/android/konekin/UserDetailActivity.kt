package com.android.konekin

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.konekin.databinding.ActivityUserDetailBinding
import com.android.konekin.model.Data
import com.android.konekin.model.UserDetail
import com.android.konekin.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class UserDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_id"
        private const val MAX_AUTO_RETRY = 3
    }

    private lateinit var binding: ActivityUserDetailBinding

    // untuk retry otomatis kalau server balas 429 (Too Many Requests)
    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // ambil id yang dikirim dari MainActivity
        val id = intent.getIntExtra(EXTRA_ID, -1)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener {
            retryCount = 0
            getUserDetail(id)
        }

        getUserDetail(id)
    }

    private fun getUserDetail(id: Int) {
        showLoading()

        // GET https://dummy.restapiexample.com/api/v1/employee/{id}
        ApiClient.getInstance().getUserById(id).enqueue(object : Callback<UserDetail> {
            override fun onResponse(call: Call<UserDetail>, response: Response<UserDetail>) {
                val user = response.body()?.data
                if (response.isSuccessful && user != null) {
                    retryCount = 0
                    showUser(user)
                } else if (response.code() == 429 && retryCount < MAX_AUTO_RETRY) {
                    // API dummy membatasi jumlah request. Tunggu sesuai header Retry-After
                    // (kalau tidak ada, pakai 5, 10, 15 detik) lalu coba lagi otomatis.
                    retryCount++
                    val delaySec = response.headers()["Retry-After"]?.toLongOrNull()
                        ?: (5L * retryCount)
                    showLoading()
                    binding.tvStatus.text =
                        "Server sibuk (429). Coba otomatis ke-$retryCount dalam $delaySec detik..."
                    binding.tvStatus.visibility = View.VISIBLE
                    handler.postDelayed({ getUserDetail(id) }, delaySec * 1000)
                } else {
                    showError("Gagal ambil detail (kode ${response.code()}). Coba lagi.")
                }
            }

            override fun onFailure(call: Call<UserDetail>, t: Throwable) {
                showError("Koneksi error: ${t.message}")
            }
        })
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun showUser(user: Data) {
        val salary = NumberFormat.getCurrencyInstance(Locale.US).format(user.employeeSalary)

        binding.tvName.text = user.employeeName
        binding.tvId.text = user.id.toString()
        binding.tvAge.text = getString(R.string.age_format, user.employeeAge)
        binding.tvSalary.text = salary

        binding.progressBar.visibility = View.GONE
        binding.tvStatus.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.cardDetail.visibility = View.VISIBLE
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.cardDetail.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.visibility = View.GONE
        binding.cardDetail.visibility = View.GONE
        binding.layoutError.visibility = View.VISIBLE
    }
}
