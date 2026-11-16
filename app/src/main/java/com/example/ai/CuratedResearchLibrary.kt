package com.example.ai

import com.example.data.GameType

data class CuratedResearchSource(
    val id: String,
    val gameType: GameType,
    val title: String,
    val sourceType: String, // "wiki", "strategy_guide", "video_transcript"
    val badgeLabel: String,
    val contentExcerpt: String
)

object CuratedResearchLibrary {
    val sources: List<CuratedResearchSource> = listOf(
        // TACTICAL GRID SOURCES
        CuratedResearchSource(
            id = "tg_wiki_1",
            gameType = GameType.TACTICAL_GRID,
            title = "Official Tactical Grid Wiki: Energy Nodes, Synergy & EMP Traps",
            sourceType = "wiki",
            badgeLabel = "GAME WIKI",
            contentExcerpt = """
                Tactical Grid is played on a 5x5 sector matrix over 16 turns against an adaptive opponent bot.
                1. Center Control: Sector (2,2) and its four orthogonal neighbors (1,2), (3,2), (2,1), (2,3) generate +35% higher base energy and enable cross-board adjacency chains.
                2. Adjacency Synergy: Capturing a tile orthogonally adjacent to an already-owned AI sector awards a +12 Synergy Bonus per connected friendly tile.
                3. EMP Hazard Zones: Red EMP trap sectors deduct -25 points and stun expansion if captured blindly. High-tier bots plant traps adjacent to high-value energy cores.
                4. Blocking Principle: If the opponent bot holds 2 contiguous tiles next to a 30+ point core, denying that core yields a higher net differential than taking an isolated edge tile.
            """.trimIndent()
        ),
        CuratedResearchSource(
            id = "tg_guide_2",
            gameType = GameType.TACTICAL_GRID,
            title = "Grandmaster Guide: Beating Tier 7–10 Adaptive Bots",
            sourceType = "strategy_guide",
            badgeLabel = "STRATEGY GUIDE",
            contentExcerpt = """
                At Difficulty Tiers 7 through 10, the opponent bot increases lookahead depth and spawns extra EMP traps near central nodes.
                - Opening Phase (Turns 1-4): Never open on a corner tile unless it holds a 40+ Quantum Core. Secure center-adjacent anchors first.
                - Mid-Game Trap Avoidance: Before capturing any tile with >=30 base energy on Tier 5+, evaluate whether an adjacent unrevealed hazard penalty would wipe out your lead.
                - Endgame Conversion (Turns 12-16): Prioritize guaranteed adjacency clusters (+24 to +36 synergy) over risky contested single nodes.
            """.trimIndent()
        ),
        CuratedResearchSource(
            id = "tg_video_3",
            gameType = GameType.TACTICAL_GRID,
            title = "Pro Match Breakdown Transcript: Why Greedy Captures Lose Games",
            sourceType = "video_transcript",
            badgeLabel = "VIDEO TRANSCRIPT",
            contentExcerpt = """
                [00:42] Watch what happens when the agent rushes the top-right 35-energy node on Turn 3 without connected anchors: it triggers an EMP trap (-25) and hands the entire center corridor to the bot.
                [02:15] In contrast, in Game 2 the agent builds an L-shaped fortress around (2,2) -> (2,3) -> (1,2). Every subsequent capture triggers double and triple adjacency bonuses, winning 245 to 160 even on Difficulty Tier 8.
            """.trimIndent()
        ),

        // 2048 NEURAL MATRIX SOURCES
        CuratedResearchSource(
            id = "n2048_wiki_1",
            gameType = GameType.NEURAL_2048,
            title = "2048 Compendium Wiki: Corner Anchor & Monotonicity Mechanics",
            sourceType = "wiki",
            badgeLabel = "GAME WIKI",
            contentExcerpt = """
                2048 Neural Matrix operates on a 4x4 grid where every slide merges identical tiles and spawns a new 2 or 4 tile in an empty cell.
                1. Corner Anchor Invariant: Keep the highest-value tile locked in the top-left corner (0,0) at all times.
                2. Row/Column Monotonicity: Arrange tiles in strictly decreasing order along the top row (e.g., 512 -> 256 -> 128 -> 64) so cascades merge cleanly from right to left.
                3. Forbidden Direction Rule: When the anchor is in the top-left (0,0) and the top row is not completely full, sliding DOWN or RIGHT risks spawning a 2-tile in (0,0) and dislodging the anchor—the #1 fatal mistake in 2048.
            """.trimIndent()
        ),
        CuratedResearchSource(
            id = "n2048_guide_2",
            gameType = GameType.NEURAL_2048,
            title = "Deep Heuristic Manual: Empty-Cell Survival Under High-Tier Pressure",
            sourceType = "strategy_guide",
            badgeLabel = "STRATEGY GUIDE",
            contentExcerpt = """
                As Adaptive Difficulty rises from Tier 1 to Tier 10, the target milestone tile scales from 128 up to 2048 and the probability of awkward 4-tile spawns increases.
                - Primary Heuristic: Prioritize moves that (a) keep the maximum tile in a corner, (b) maximize immediate merges to keep at least 4 empty cells open, and (c) align adjacent equal-value tiles.
                - Emergency Recovery: If the board drops below 3 empty cells, prioritize any move that executes 2+ simultaneous merges over strict snake ordering.
            """.trimIndent()
        ),
        CuratedResearchSource(
            id = "n2048_video_3",
            gameType = GameType.NEURAL_2048,
            title = "AI Speedrun Transcript: Avoiding the Flat-Board Lockout",
            sourceType = "video_transcript",
            badgeLabel = "VIDEO TRANSCRIPT",
            contentExcerpt = """
                [01:10] Notice how novice bots alternate UP-DOWN-LEFT-RIGHT randomly and checker the board with alternating 4s and 8s that can never merge.
                [03:05] By restricting 92% of moves to UP and LEFT, and only using RIGHT when row 0 is locked with 4 tiles, the agent maintains a clean monotonic gradient and hits the target tile with zero deadlocks.
            """.trimIndent()
        ),

        // CYBER SNAKE ARENA SOURCES
        CuratedResearchSource(
            id = "cs_wiki_1",
            gameType = GameType.CYBER_SNAKE,
            title = "Cyber Snake Arena Wiki: Pathfinding, Tail Space & Firewall Nodes",
            sourceType = "wiki",
            badgeLabel = "GAME WIKI",
            contentExcerpt = """
                Cyber Snake Arena takes place on an 8x8 grid containing the AI snake, high-value Data Cores (+20 pts), and static/shifting Firewall Obstacles.
                1. Manhattan Shortest Path vs. Trap Risk: Moving directly toward a Data Core decreases Manhattan distance, but entering a dead-end pocket surrounded by the snake's own body or Firewall walls causes fatal collision.
                2. Flood-Fill Reachable Space: Before committing to any direction (UP, DOWN, LEFT, RIGHT), count the number of reachable open cells from the target head position. If reachable space is smaller than the snake's body length, the move is a trap!
                3. Center-Bias Survival: Wall-hugging along the outer perimeter reduces available escape directions from 3 down to 1 or 2.
            """.trimIndent()
        ),
        CuratedResearchSource(
            id = "cs_guide_2",
            gameType = GameType.CYBER_SNAKE,
            title = "Hamiltonian & Flood-Fill Survival Guide for High Hazard Density",
            sourceType = "strategy_guide",
            badgeLabel = "STRATEGY GUIDE",
            contentExcerpt = """
                On Difficulty Tiers 5–10, extra Firewall blocks spawn on the 8x8 arena and the required Data Core quota increases.
                - Rule 1 (Safety First): Never step onto a cell with fewer than 2 open exit neighbors unless it immediately collects the final winning Data Core.
                - Rule 2 (Tail Chasing): When the direct route to the Data Core is blocked by the snake's mid-body, route toward the moving tail tip to coils-unroll safely until a clean corridor opens.
            """.trimIndent()
        )
    )

    fun getSourcesForGame(gameType: GameType): List<CuratedResearchSource> =
        sources.filter { it.gameType == gameType }
}
