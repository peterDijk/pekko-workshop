package com.aw.nft.asset

import com.aw.nft.asset.entity.NFTAssetEntity
import com.aw.nft.asset.entity.NFTAssetEntity.{AssetCommand, CreateAsset, GetAsset, AddFileIdToAsset}
import com.aw.nft.asset.model.NFTAsset
import com.aw.nft.grpc.*
import com.google.protobuf.empty.Empty
import com.google.protobuf.timestamp.Timestamp
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import org.apache.pekko.grpc.scaladsl.Metadata
import org.apache.pekko.util.Timeout
import org.slf4j.LoggerFactory

import java.net.InetAddress
import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class NFTAssetServiceImpl[A: ActorSystem]() extends NFTAssetServicePowerApi:

  val system   = summon[ActorSystem[?]]
  val log      = LoggerFactory.getLogger(getClass)
  val sharding = ClusterSharding(system)

  given ec: ExecutionContext = system.executionContext

  given timeout: Timeout = Timeout.create(system.settings.config.getDuration("nft-asset-svc.grpc.ask-timeout"))

  private val hostname = InetAddress.getLocalHost.getHostAddress

  override def getHealth(in: Empty, metadata: Metadata): Future[GetHealthResponse] =
    Future.successful(
      GetHealthResponse("NFTAsset gRPC is healthy!", hostname, Some(Timestamp(Instant.now())))
    )

  override def createNFTAsset(in: CreateNFTAssetRequest, metadata: Metadata): Future[CreateNFTAssetResponse] = {
    val assetId = UUID.randomUUID.toString
    val newAsset = NFTAsset(
      id = assetId,
      name = in.assetName,
      description = in.assetDescription
    )
    createNewAsset(newAsset).recoverWith { case e =>
      log.error(s"Failed to create NFT Asset: ${e.getMessage}")
      Future.failed(e)
    }
    .map(_ => CreateNFTAssetResponse(assetId))
  }

  override def getNFTAsset(in: GetNFTAssetRequest, metadata: Metadata): Future[GetNFTAssetResponse] = {
    getAsset(in.assetId)
      .recoverWith { case e =>
        log.error(s"Failed to get NFT Asset: ${e.getMessage}")
        Future.failed(e)
      }
      .map(asset => GetNFTAssetResponse(
        assetId = asset.id,
        assetName = asset.name,
        assetDescription = asset.description,
        assetFileId = Some(asset.fileId.getOrElse("not added yet")))
      )
  }

  override def addNFTFileId(in: AddNFTFileIdRequest, metadata: Metadata): Future[AddNFTFileIdResponse] = {
    addFileId(in.assetId, in.assetFileId)
      .recoverWith { case e =>
        log.error(s"Failed to add fileId to NFT Asset: ${e.getMessage}")
        Future.failed(e)
      }
      .map(_ => AddNFTFileIdResponse(assetId = in.assetId, message = "fileId added success"))
  }

  override def renameNFTAsset(in: RenameNFTAssetRequest, metadata: Metadata): Future[RenameNFTAssetResponse] = ???

  override def removeNFTAsset(in: RemoveNFTAssetRequest, metadata: Metadata): Future[RemoveNFTAssetResponse] = ???

  private def entityRef(assetId: String): EntityRef[AssetCommand] =
    sharding.entityRefFor(NFTAssetEntity.EntityKey, assetId)

  protected def getAsset(assetId: String): Future[NFTAsset] =
    entityRef(assetId).askWithStatus[NFTAsset](ref => GetAsset(assetId, ref))
  //    gets a reference to the entity from the sharded system - by id -
  //    (so from any of the nodes, where ever this entity lives, the orchestrator knows where?
  // then runs the command on that entity-ref , but to pass id again. Why?
  //

  protected def createNewAsset(asset: NFTAsset): Future[Done] =
    entityRef(asset.id).askWithStatus[Done](ref => CreateAsset(asset, ref))
    // entity does not exist yet in cluster?
    // does a new reference get created (getOrCreate)?

  protected def addFileId(assetId: String, fileId: String): Future[Done] =
    entityRef(assetId).askWithStatus[Done](ref => AddFileIdToAsset(assetId = assetId, fileId = fileId, replyTo = ref))