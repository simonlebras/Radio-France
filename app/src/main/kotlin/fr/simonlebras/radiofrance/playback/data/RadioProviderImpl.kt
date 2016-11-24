package fr.simonlebras.radiofrance.playback.data

import android.support.v4.media.MediaMetadataCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import fr.simonlebras.radiofrance.playback.mappers.MediaMetadataMapper
import fr.simonlebras.radiofrance.utils.DebugUtils
import fr.simonlebras.radiofrance.utils.FirebaseUtils
import fr.simonlebras.radiofrance.utils.LogUtils
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@ServiceScope
class RadioProviderImpl @Inject constructor(val mapper: MediaMetadataMapper) : RadioProvider {
    private companion object {
        private val TAG = LogUtils.makeLogTag(RadioProviderImpl::class.java.simpleName)
        private const val RADIOS_KEY = "radios"
    }

    override val radios: Flowable<List<MediaMetadataCompat>> by lazy(LazyThreadSafetyMode.NONE) {
        Flowable
                .create<DataSnapshot>({
                    FirebaseUtils.enableFirebaseDatabasePersistence()
                    val database = FirebaseDatabase.getInstance().reference

                    val listener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (!it.isCancelled) {
                                it.onNext(dataSnapshot)
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            DebugUtils.executeInDebugMode {
                                Timber.e(TAG, databaseError.details)
                            }
                        }
                    }

                    it.setCancellable {
                        database.removeEventListener(listener)
                    }

                    database.child(RADIOS_KEY).addValueEventListener(listener)
                }, BackpressureStrategy.LATEST)
                .observeOn(Schedulers.computation())
                .map {
                    mapper.transform(it.children)
                }
                .replay(1)
                .autoConnect(0) {
                    compositeDisposable.add(it)
                }
    }

    private val compositeDisposable = CompositeDisposable()

    override fun reset() {
        compositeDisposable.clear()
    }
}
