/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.modem.TileCable;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlockCable extends BlockPeripheralBase
{
    public static final PropertyEnum<BlockCableModemVariant> MODEM = PropertyEnum.create( "modem", BlockCableModemVariant.class );
    public static final PropertyEnum<BlockCableCableVariant> CABLE = PropertyEnum.create( "cable", BlockCableCableVariant.class );
    public static final PropertyBool NORTH = PropertyBool.create( "north" );
    public static final PropertyBool SOUTH = PropertyBool.create( "south" );
    public static final PropertyBool EAST = PropertyBool.create( "east" );
    public static final PropertyBool WEST = PropertyBool.create( "west" );
    public static final PropertyBool UP = PropertyBool.create( "up" );
    public static final PropertyBool DOWN = PropertyBool.create( "down" );

    // Members

    public BlockCable()
    {
        setHardness( 1.5f );
        setTranslationKey( "computercraft:cable" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( this.blockState.getBaseState()
            .withProperty( MODEM, BlockCableModemVariant.None )
            .withProperty( CABLE, BlockCableCableVariant.NONE )
            .withProperty( NORTH, false )
            .withProperty( SOUTH, false )
            .withProperty( EAST, false )
            .withProperty( WEST, false )
            .withProperty( UP, false )
            .withProperty( DOWN, false )
        );
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this,
            MODEM,
            CABLE,
            NORTH,
            SOUTH,
            EAST,
            WEST,
            UP,
            DOWN
        );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta( int meta )
    {
        IBlockState state = getDefaultState();
        if( meta < 6 )
        {
            state = state.withProperty( CABLE, BlockCableCableVariant.NONE );
            state = state.withProperty( MODEM, BlockCableModemVariant.fromFacing( EnumFacing.byIndex( meta ) ) );
        }
        else if( meta < 12 )
        {
            state = state.withProperty( CABLE, BlockCableCableVariant.ANY );
            state = state.withProperty( MODEM, BlockCableModemVariant.fromFacing( EnumFacing.byIndex( meta - 6 ) ) );
        }
        else if( meta == 13 )
        {
            state = state.withProperty( CABLE, BlockCableCableVariant.ANY );
            state = state.withProperty( MODEM, BlockCableModemVariant.None );
        }
        return state;
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        int meta = 0;
        boolean cable = state.getValue( CABLE ) != BlockCableCableVariant.NONE;
        BlockCableModemVariant modem = state.getValue( MODEM );
        if( cable && modem != BlockCableModemVariant.None )
        {
            meta = 6 + modem.getFacing().getIndex();
        }
        else if( modem != BlockCableModemVariant.None )
        {
            meta = modem.getFacing().getIndex();
        }
        else if( cable )
        {
            meta = 13;
        }
        return meta;
    }

    @Override
    public IBlockState getDefaultBlockState( PeripheralType type, EnumFacing placedSide )
    {
        switch( type )
        {
            case Cable:
            {
                return getDefaultState()
                    .withProperty( CABLE, BlockCableCableVariant.ANY )
                    .withProperty( MODEM, BlockCableModemVariant.None );
            }
            case WiredModem:
            default:
            {
                return getDefaultState()
                    .withProperty( CABLE, BlockCableCableVariant.NONE )
                    .withProperty( MODEM, BlockCableModemVariant.fromFacing( placedSide.getOpposite() ) );
            }
            case WiredModemWithCable:
            {
                return getDefaultState()
                    .withProperty( CABLE, BlockCableCableVariant.ANY )
                    .withProperty( MODEM, BlockCableModemVariant.fromFacing( placedSide.getOpposite() ) );
            }
        }
    }

    public static boolean canConnectIn( IBlockState state, EnumFacing direction )
    {
        return state.getValue( BlockCable.CABLE ) != BlockCableCableVariant.NONE
            && state.getValue( BlockCable.MODEM ).getFacing() != direction;
    }

    public static boolean doesConnectVisually( IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing direction )
    {
        if( state.getValue( CABLE ) == BlockCableCableVariant.NONE ) return false;
        if( state.getValue( MODEM ).getFacing() == direction ) return true;
        return ComputerCraft.getWiredElementAt( world, pos.offset( direction ), direction.getOpposite() ) != null;
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess world, BlockPos pos )
    {
        state = state.withProperty( NORTH, doesConnectVisually( state, world, pos, EnumFacing.NORTH ) );
        state = state.withProperty( SOUTH, doesConnectVisually( state, world, pos, EnumFacing.SOUTH ) );
        state = state.withProperty( EAST, doesConnectVisually( state, world, pos, EnumFacing.EAST ) );
        state = state.withProperty( WEST, doesConnectVisually( state, world, pos, EnumFacing.WEST ) );
        state = state.withProperty( UP, doesConnectVisually( state, world, pos, EnumFacing.UP ) );
        state = state.withProperty( DOWN, doesConnectVisually( state, world, pos, EnumFacing.DOWN ) );

        if( state.getValue( CABLE ) != BlockCableCableVariant.NONE )
        {
            BlockCableCableVariant direction = null;
            if( state.getValue( WEST ) || state.getValue( EAST ) )
            {
                direction = direction == null ? BlockCableCableVariant.X_AXIS : BlockCableCableVariant.ANY;
            }
            if( state.getValue( DOWN ) || state.getValue( UP ) )
            {
                direction = direction == null ? BlockCableCableVariant.Y_AXIS : BlockCableCableVariant.ANY;
            }
            if( state.getValue( NORTH ) || state.getValue( SOUTH ) )
            {
                direction = direction == null ? BlockCableCableVariant.Z_AXIS : BlockCableCableVariant.ANY;
            }

            state = state.withProperty( CABLE, direction == null ? BlockCableCableVariant.Z_AXIS : direction );
        }

        int anim;
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TilePeripheralBase )
        {
            TilePeripheralBase peripheral = (TilePeripheralBase) tile;
            anim = peripheral.getAnim();
        }
        else
        {
            anim = 0;
        }

        BlockCableModemVariant modem = state.getValue( MODEM );
        if( modem != BlockCableModemVariant.None )
        {
            modem = BlockCableModemVariant.values()[
                1 + 6 * anim + modem.getFacing().getIndex()
                ];
        }
        state = state.withProperty( MODEM, modem );

        return state;
    }

    @Override
    @Deprecated
    public boolean shouldSideBeRendered( IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, EnumFacing side )
    {
        return true;
    }

    @Override
    public PeripheralType getPeripheralType( int damage )
    {
        return ((ItemCable) Item.getItemFromBlock( this )).getPeripheralType( damage );
    }

    @Override
    public PeripheralType getPeripheralType( IBlockState state )
    {
        boolean cable = state.getValue( CABLE ) != BlockCableCableVariant.NONE;
        BlockCableModemVariant modem = state.getValue( MODEM );
        if( cable && modem != BlockCableModemVariant.None )
        {
            return PeripheralType.WiredModemWithCable;
        }
        else if( modem != BlockCableModemVariant.None )
        {
            return PeripheralType.WiredModem;
        }
        else
        {
            return PeripheralType.Cable;
        }
    }

    @Override
    public TilePeripheralBase createTile( PeripheralType type )
    {
        return new TileCable();
    }

    @Nullable
    @Override
    @Deprecated
    public RayTraceResult collisionRayTrace( IBlockState blockState, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Vec3d start, @Nonnull Vec3d end )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileCable && tile.hasWorld() )
        {
            TileCable cable = (TileCable) tile;

            double distance = Double.POSITIVE_INFINITY;
            RayTraceResult result = null;

            List<AxisAlignedBB> bounds = new ArrayList<AxisAlignedBB>( 7 );
            cable.getCollisionBounds( bounds );

            Vec3d startOff = start.subtract( pos.getX(), pos.getY(), pos.getZ() );
            Vec3d endOff = end.subtract( pos.getX(), pos.getY(), pos.getZ() );

            for( AxisAlignedBB bb : bounds )
            {
                RayTraceResult hit = bb.calculateIntercept( startOff, endOff );
                if( hit != null )
                {
                    double newDistance = hit.hitVec.squareDistanceTo( startOff );
                    if( newDistance <= distance )
                    {
                        distance = newDistance;
                        result = hit;
                    }
                }
            }

            return result == null ? null : new RayTraceResult( result.hitVec.add( pos.getX(), pos.getY(), pos.getZ() ), result.sideHit, pos );
        }
        else
        {
            return super.collisionRayTrace( blockState, world, pos, start, end );
        }
    }

    @Override
    @Deprecated
    public final void addCollisionBoxToList( IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull AxisAlignedBB bigBox, @Nonnull List<AxisAlignedBB> list, Entity entity, boolean p_185477_7_ )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileCable && tile.hasWorld() )
        {
            TileCable cable = (TileCable) tile;

            // Get collision bounds
            List<AxisAlignedBB> collision = new ArrayList<>( 1 );
            cable.getCollisionBounds( collision );

            // Add collision bounds to list
            for( AxisAlignedBB localBounds : collision ) addCollisionBoxToList( pos, bigBox, list, localBounds );
        }
    }

    @Override
    public boolean removedByPlayer( @Nonnull IBlockState state, World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest )
    {
        PeripheralType type = getPeripheralType( world, pos );
        if( type == PeripheralType.WiredModemWithCable )
        {
            RayTraceResult hit = state.collisionRayTrace( world, pos, WorldUtil.getRayStart( player ), WorldUtil.getRayEnd( player ) );
            if( hit != null )
            {
                TileEntity tile = world.getTileEntity( pos );
                if( tile instanceof TileCable && tile.hasWorld() )
                {
                    TileCable cable = (TileCable) tile;

                    ItemStack item;

                    AxisAlignedBB bb = cable.getModemBounds();
                    if( WorldUtil.isVecInsideInclusive( bb, hit.hitVec.subtract( pos.getX(), pos.getY(), pos.getZ() ) ) )
                    {
                        world.setBlockState( pos, state.withProperty( MODEM, BlockCableModemVariant.None ), 3 );
                        item = PeripheralItemFactory.create( PeripheralType.WiredModem, null, 1 );
                    }
                    else
                    {
                        world.setBlockState( pos, state.withProperty( CABLE, BlockCableCableVariant.NONE ), 3 );
                        item = PeripheralItemFactory.create( PeripheralType.Cable, null, 1 );
                    }

                    cable.modemChanged();
                    cable.connectionsChanged();
                    if( !world.isRemote && !player.capabilities.isCreativeMode ) dropItem( world, pos, item );

                    return false;
                }
            }
        }

        return super.removedByPlayer( state, world, pos, player, willHarvest );
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock( @Nonnull IBlockState state, RayTraceResult hit, @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileCable && tile.hasWorld() )
        {
            TileCable cable = (TileCable) tile;
            PeripheralType type = getPeripheralType( state );

            if( type == PeripheralType.WiredModemWithCable )
            {
                if( hit == null || WorldUtil.isVecInsideInclusive( cable.getModemBounds(), hit.hitVec.subtract( pos.getX(), pos.getY(), pos.getZ() ) ) )
                {
                    return PeripheralItemFactory.create( PeripheralType.WiredModem, null, 1 );
                }
                else
                {
                    return PeripheralItemFactory.create( PeripheralType.Cable, null, 1 );
                }
            }
            else
            {
                return PeripheralItemFactory.create( type, null, 1 );
            }
        }

        return PeripheralItemFactory.create( PeripheralType.Cable, null, 1 );
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TileCable )
        {
            TileCable cable = (TileCable) tile;
            if( cable.getPeripheralType() != PeripheralType.WiredModem )
            {
                cable.connectionsChanged();
            }
        }

        super.onBlockPlacedBy( world, pos, state, placer, stack );
    }

    @Override
    @Deprecated
    public final boolean isOpaqueCube( IBlockState state )
    {
        return false;
    }

    @Override
    @Deprecated
    public final boolean isFullCube( IBlockState state )
    {
        return false;
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockFaceShape getBlockFaceShape( IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing side )
    {
        return BlockFaceShape.UNDEFINED;
    }
}
