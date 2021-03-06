package com.abduhanifan.dicoding.githubuserapp

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.abduhanifan.dicoding.githubuserapp.adapter.TabsPagerAdapter
import com.abduhanifan.dicoding.githubuserapp.db.DatabaseContract.FavoriteColumns.Companion.AVATAR_URL
import com.abduhanifan.dicoding.githubuserapp.db.DatabaseContract.FavoriteColumns.Companion.LOGIN
import com.abduhanifan.dicoding.githubuserapp.db.DatabaseContract.FavoriteColumns.Companion.TYPE
import com.abduhanifan.dicoding.githubuserapp.db.FavoriteHelper
import com.abduhanifan.dicoding.githubuserapp.model.DetailUserItem
import com.abduhanifan.dicoding.githubuserapp.model.UserItem
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import cz.msebera.android.httpclient.Header
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.layout_tabs.*
import org.json.JSONObject

class DetailActivity : AppCompatActivity() {

//    private val listDetailUser = MutableLiveData<ArrayList<DetailUserItem>>()
//    private lateinit var detailViewModel: DetailViewModel
    private lateinit var favoriteHelper: FavoriteHelper

    private lateinit var username: String
    private lateinit var avatarUrl: String
    private lateinit var type: String

    private var menuItem: Menu? = null
    private var statusFavorite: Boolean = false

    companion object {
        const val EXTRA_USER = "extra_user"
        const val EXTRA_LOGIN = "extra_login"
        const val EXTRA_AVATAR = "extra_avatar_url"
        const val EXTRA_TYPE = "extra_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        supportActionBar?.setTitle(R.string.detail_label)
        supportActionBar?.elevation = 0F

        val username = intent.getParcelableExtra(EXTRA_USER) as UserItem
        getDetailUser(username.login.toString())

        favoriteHelper = FavoriteHelper.getInstance(applicationContext)
        favoriteHelper.open()

        Glide.with(this)
            .load(username.avatar_url)
            .apply(RequestOptions().override(86, 86))
            .into(imgAvatar)

//        val username = intent?.getStringExtra(EXTRA_LOGIN) as DetailUserItem
//        detailViewModel.setDetailUser(username.login.toString())
//
//        detailViewModel.getDetailUser().observe(this, Observer { detailUserItem ->
//            if (detailUserItem != null) {
//                Glide.with(this)
//                    .load(detailUserItem[0].avatar_url)
//                    .apply(RequestOptions().override(86, 86))
//                    .into(imgAvatar)
//                textName.text = detailUserItem[0].name.toString()
//                textCompany.text = detailUserItem[0].company.toString()
//                textLocation.text = detailUserItem[0].location.toString()
//                textRepository.text = detailUserItem[0].public_repos.toString()
//                textFollower.text = detailUserItem[0].followers.toString()
//                textFollowing.text = detailUserItem[0].following.toString()
//            }
//        })

        showTabs()
        favoriteState()
    }

    private fun getDetailUser(username: String) {
        val listItem = ArrayList<DetailUserItem>()

        val url = "https://api.github.com/users/$username"

        val client = AsyncHttpClient()
        client.addHeader("Authorization", "9eb6c532fc801bae706df9f0b12775c0c4ffeea4")
        client.addHeader("User-Agent", "request")
        client.get(url, object : AsyncHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<Header>,
                responseBody: ByteArray
            ) {
                try {
                    //Parsing json
                    val result = String(responseBody)
                    val jsonObject = JSONObject(result)
                    val detailUserItem = DetailUserItem()

                    detailUserItem.apply {
                        textName.text = jsonObject.getString("name")
                        textLocation.text = jsonObject.getString("location")
                        textCompany.text = jsonObject.getString("company")
                        textRepository.text = jsonObject.getString("public_repos")
                        textFollower.text = jsonObject.getString("followers")
                        textFollowing.text = jsonObject.getString("following")
                    }
                    listItem.add(detailUserItem)

//                    listDetailUser.postValue(listItem)
                } catch (e: Exception) {
                    Log.d("Exception", e.message.toString())
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<Header>?,
                responseBody: ByteArray?,
                error: Throwable?
            ) {
                Log.d("onFailure", error?.message.toString())
            }
        })
    }

//    private fun setDetail(detailItem: DetailUserItem?) {
//        val tvDetailName: TextView = findViewById(R.id.textName)
//        val tvDetailFollowers: TextView = findViewById(R.id.textFollower)
//        val tvDetailFollowing: TextView = findViewById(R.id.textFollowing)
//        val tvDetailCompany: TextView = findViewById(R.id.textCompany)
//        val tvDetailLocation: TextView = findViewById(R.id.textLocation)
//        val tvDetailRepository: TextView = findViewById(R.id.textRepository)
//
//        Glide.with(this)
//            .load(detailItem?.avatar_url)
//            .apply(RequestOptions().override(86, 86))
//            .into(imgAvatar)
//        tvDetailName.text = detailItem?.username
//        tvDetailLocation.text = detailItem?.location
//        tvDetailCompany.text = detailItem?.company
//        tvDetailRepository.text = detailItem?.public_repos.toString()
//        tvDetailFollowers.text = detailItem?.followers.toString()
//        tvDetailFollowing.text = detailItem?.following.toString()
//    }

    private fun favoriteState() {
        username = intent?.getStringExtra(EXTRA_LOGIN).toString()
        val result = favoriteHelper.queryByLogin(username)
        val favorite = (1 .. result.count).map {
            result.apply {
                moveToNext()
                getInt(result.getColumnIndexOrThrow(LOGIN))
            }
        }
        if (favorite.isNotEmpty()) statusFavorite = true
    }

    // Menambahkan data Favorite user ke SQLite
    private fun addFavorite() {
        try {
            username = intent?.getStringExtra(EXTRA_LOGIN).toString()
            avatarUrl = intent?.getStringExtra(EXTRA_AVATAR).toString()
            type = intent?.getStringExtra(EXTRA_TYPE).toString()

            val values = ContentValues().apply {
                put(LOGIN, username)
                put(AVATAR_URL, avatarUrl)
                put(TYPE, type)
            }
            favoriteHelper.insert(values)

            showSnackbarMessage("Menambahkan Favorite User")
            Log.d("Masukan Nilai ::::: ", values.toString())
        } catch (e: SQLiteConstraintException) {
            showSnackbarMessage(""+e.localizedMessage)
        }
    }

    // Menghapus data Favorite user dari SQLite
    private fun removeFavorite() {
        try {
            username = intent?.getStringExtra(EXTRA_LOGIN).toString()
            val result = favoriteHelper.deleteByLogin(username)

            showSnackbarMessage("Menghapus Favorite User")
            Log.d("Hapus nilai ::::: ", result.toString())
        } catch (e: SQLiteConstraintException) {
            showSnackbarMessage(""+e.localizedMessage)
        }
    }

//    private fun setFavorite() {
//            if (statusFavorite) {
//                btnFavoriteUser.setImageResource(R.drawable.ic_baseline_favorite_24)
//            } else {
//                btnFavoriteUser.setImageResource(R.drawable.ic_baseline_favorite_border_24)
//            }
//    }

    private fun setFavorite() {
        if (statusFavorite) {
            menuItem?.getItem(0)?.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_favorite_24)
        } else {
            menuItem?.getItem(0)?.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_favorite_border_24)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        menuItem = menu
        setFavorite()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_setting -> {
                val intent = Intent(this, SettingActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_favorite -> {
                if (statusFavorite) removeFavorite() else addFavorite()

                statusFavorite = !statusFavorite
                setFavorite()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

//    override fun onSupportNavigateUp(): Boolean {
//        super.onBackPressed()
//        return true
//    }

    private fun showTabs() {
        val sectionsPagerAdapter = TabsPagerAdapter(this, supportFragmentManager)
        viewPager.adapter = sectionsPagerAdapter
        tabs.setupWithViewPager(viewPager)
    }

    // Tampilkan snackbar
    private fun showSnackbarMessage(message: String) {
        Snackbar.make(viewPager, message, Snackbar.LENGTH_SHORT).show()
    }
}
