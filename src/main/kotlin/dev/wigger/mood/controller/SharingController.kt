package dev.wigger.mood.controller

import dev.wigger.mood.dto.*
import dev.wigger.mood.entry.EntryService
import dev.wigger.mood.shareing.Sharing
import dev.wigger.mood.shareing.SharingService
import dev.wigger.mood.user.UserService
import dev.wigger.mood.util.mapper.WebApplicationMapperException
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.SecurityContext
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme
import java.util.UUID

@ApplicationScoped
@Path("/") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
)
class SharingController {
    @Inject
    private lateinit var sharingService: SharingService
    
    @Inject
    private lateinit var userService: UserService
    
    @Inject
    private lateinit var entryService: EntryService
    
    @GET @Path("/sharing/delegator")
    @RolesAllowed("USER")
    fun delegator(ctx: SecurityContext): List<SharingDelegatorDto> = sharingService.findByUserId(UUID.fromString(ctx.userPrincipal.name)).map { sharing ->
        sharing.apply {
            entry = entryService.findByUserIdPermission(sharing.delegator.id, sharing.permissions)
        }.toDelegatorDto()
    }

    @GET @Path("/sharing/token/create")
    @RolesAllowed("USER")
    @Transactional
    fun createToken(ctx: SecurityContext): SharingTokenDto {
        val user = userService.findByIdUuid(UUID.fromString(ctx.userPrincipal.name))
        val token = UUID.randomUUID()
        
        userService.updateOne(
            user.id,
            user.apply { sharingToken = token },
        )
        
        return SharingTokenDto(token)
    }
    
    @POST @Path("/sharing/token/connect")
    @RolesAllowed("USER")
    @Transactional
    fun connectToken(@Valid payload: SharingSubmittDto, ctx: SecurityContext) {
        val users = userService.findByIdUuid(UUID.fromString(ctx.userPrincipal.name))
        val delegators = userService.findBySharingToken(payload.token)

        if (users.id == delegators.id) {
            throw WebApplicationMapperException("Cannot connect the same user", 422)
        }
        
        sharingService.findByUserIdAndDelegatorId(users.id, delegators.id)?.let {
            throw WebApplicationMapperException("Cannot connect a user twice", 422)
        }

        sharingService.persistOne(Sharing().apply {
            user = users
            delegator = delegators
            permissions = payload.permissions
        })
    }
    
    @DELETE @Path("/sharing/{id}")
    @RolesAllowed("USER")
    @Transactional
    fun delete(id: UUID, ctx: SecurityContext) {
        sharingService.findByUserIdAndDelegatorId(UUID.fromString(ctx.userPrincipal.name), id)
            ?: throw WebApplicationMapperException("User and delegator do not share anything at the moment", 422)
        
        sharingService.delete(UUID.fromString(ctx.userPrincipal.name), id)
    }
    
    @PUT @Path("/sharing/{id}")
    @RolesAllowed("USER")
    @Transactional
    fun update(
        @Valid payload: SharingUpdateDto,
        id: UUID,
        ctx: SecurityContext,
    ) {
        val sharing = sharingService.findByUserIdAndDelegatorId(UUID.fromString(ctx.userPrincipal.name), id)
            ?: throw WebApplicationMapperException("User and delegator do not share anything at the moment", 422)

        sharingService.updateOne(
            UUID.fromString(ctx.userPrincipal.name),
            id,
            sharing.apply {
                permissions = payload.permissions
            },
        )
    }
}
