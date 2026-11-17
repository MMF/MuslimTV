package com.mfouad.muslimtv.core.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
@UnstableApi
object MediaModule {
    /** Everything played from the network stays here, so replaying a surah needs no network. */
    const val PLAY_CACHE_BYTES = 500L * 1024 * 1024

    @Provides
    @Singleton
    fun playCache(@ApplicationContext context: Context): SimpleCache = SimpleCache(
        File(context.cacheDir, "audio-cache"),
        LeastRecentlyUsedCacheEvictor(PLAY_CACHE_BYTES),
        StandaloneDatabaseProvider(context),
    )

    @Provides
    @Singleton
    fun dataSourceFactory(cache: SimpleCache, okHttp: OkHttpClient): DataSource.Factory {
        val network = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(okHttp).setUserAgent("MuslimTV"))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return RoutingDataSource.Factory(network, FileDataSource.Factory())
    }
}

/** Reads downloaded files directly and everything else through the play cache. */
@UnstableApi
internal class RoutingDataSource(
    private val network: DataSource,
    private val file: DataSource,
) : DataSource {
    private var active: DataSource? = null

    class Factory(private val network: DataSource.Factory, private val file: DataSource.Factory) : DataSource.Factory {
        override fun createDataSource(): DataSource = RoutingDataSource(network.createDataSource(), file.createDataSource())
    }

    override fun addTransferListener(transferListener: TransferListener) {
        network.addTransferListener(transferListener)
        file.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val source = if (dataSpec.uri.scheme == "file") file else network
        active = source
        return source.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = active!!.read(buffer, offset, length)

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = active?.responseHeaders ?: emptyMap()

    override fun close() {
        active?.close()
        active = null
    }
}
