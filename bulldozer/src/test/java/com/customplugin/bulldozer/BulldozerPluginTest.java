package com.customplugin.bulldozer;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Minecart;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class BulldozerPluginTest {

    private ServerMock server;
    private BulldozerPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(BulldozerPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testPluginLoads() {
        assertNotNull(plugin);
        assertEquals("BulldozerPlugin", plugin.getName());
    }

    @Test
    void testBulldozerCommandSpawnsCart() {
        PlayerMock player = server.addPlayer();
        boolean result = server.dispatchCommand(player, "bulldozer");
        assertTrue(result);

        var entities = player.getWorld().getEntitiesByClass(Minecart.class);
        assertEquals(1, entities.size());

        Minecart cart = entities.iterator().next();
        assertTrue(cart.getPassengers().contains(player));
        assertEquals("Bulldozer", cart.getCustomName());
    }

    @Test
    void testClearableMaterials() {
        Minecart cart = server.addSimpleWorld("world").spawn(server.getOverworld().getSpawnLocation(), Minecart.class);
        BulldozerEntity entity = new BulldozerEntity(plugin, cart);

        assertTrue(entity.isClearable(Material.DIRT));
        assertTrue(entity.isClearable(Material.GRASS_BLOCK));
        assertTrue(entity.isClearable(Material.SAND));
        assertTrue(entity.isClearable(Material.SNOW));

        assertFalse(entity.isClearable(Material.STONE));
        assertFalse(entity.isClearable(Material.WATER));
    }

    @Test
    void testBlockClearedInFront() {
        var world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        // place block in front of bulldozer
        Block target = world.getBlockAt(player.getLocation().add(0, 0, 1));
        target.setType(Material.DIRT);

        server.dispatchCommand(player, "bulldozer");
        server.getScheduler().performTicks(10); // let tasks run

        assertEquals(Material.AIR, target.getType(), "Block should have been cleared by bulldozer");
    }
}
