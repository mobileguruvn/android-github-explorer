package com.brian.users.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.brian.users.data.mapper.UserMapper
import com.brian.users.data.network.datasource.UsersRemoteDataSource
import com.brian.users.domain.model.User
import com.brian.users.domain.model.UserDetail
import com.brian.users.domain.repository.UserRepository
import com.githubbroswer.common.Dispatcher
import com.githubbroswer.common.GitHubDispatcher
import com.githubbrowser.database.UsersLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val ITEMS_PER_PAGE = 20

@OptIn(ExperimentalPagingApi::class)
class UsersRepositoryImpl @Inject constructor(
    private val usersRemoteDataSource: UsersRemoteDataSource,
    private val usersLocalDataSource: UsersLocalDataSource,
    private val usersRemoteMediator: UsersRemoteMediator,
    private val userMapper: UserMapper,
    @Dispatcher(GitHubDispatcher.IO)
    private val ioDispatcher: CoroutineDispatcher,
) : UserRepository {
    override fun getUsers(): Flow<PagingData<User>> {
        return Pager(
            config = PagingConfig(
                pageSize = ITEMS_PER_PAGE,
            ),
            remoteMediator = usersRemoteMediator,
            pagingSourceFactory = {
                usersLocalDataSource.getUsers()
            }
        ).flow.map { pagingData ->
            pagingData.map {
                userMapper.toUser(it)
            }
        }.flowOn(ioDispatcher)
    }

    override fun getUserDetail(userName: String): Flow<Result<UserDetail>> = flow {
        try {
            val response = usersRemoteDataSource.fetchUserDetail(userName)
            if (response.isFailure) {
                emit(Result.failure(response.exceptionOrNull() ?: Exception("Unknown error")))
                return@flow
            }

            val userDetail = response.getOrNull()
            if (null == userDetail) {
                emit(Result.failure(NullPointerException("User detail is null")))
                return@flow
            }

            // Save to local db
            usersLocalDataSource.insertUserDetails(
                userMapper.toUserDetailEntity(
                    userDetail
                )
            )

            // Emit from local db
            emitAll(
                usersLocalDataSource.getUserWithDetail(id = userDetail.id)
                    .map {
                        Result.success(userMapper.toUserDetail(it))
                    })

        } catch (ex: Exception) {
            emit(Result.failure(ex))
        }
    }.flowOn(ioDispatcher)
}