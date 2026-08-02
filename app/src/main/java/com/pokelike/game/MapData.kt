package com.pokelike.game

/**
 * Alle Karten der Region Auronia inklusive Bewohner, Trainer und Bosse.
 */
object MapData {

    private val shopA = listOf("fangball", "trank", "gegengift", "paraheiler", "aufwecker", "schutz")
    private val shopB = shopA + listOf("superball", "supertrank", "brandsalbe", "eisspray")
    private val shopC = shopB + listOf("hyperball", "hypertrank", "beleber", "xangriff", "xabwehr")
    private val shopD = shopC + listOf("toptrank", "superschutz", "elixier", "xspezial", "xtempo", "fluchtseil")
    private val shopE = shopD + listOf("topbeleber", "feuerstein", "wasserstein", "blattstein", "donnerstein")
    private val shopF = shopE + listOf("sonderbonbon", "kpplus", "kraftplus", "panzerplus", "geistplus", "nervenplus", "tempoplus")

    // ==================================================================
    //  Dorf Ahornfels
    // ==================================================================
    private val heim = GameMap(
        id = "heim", name = "Ahornfels",
        rowsRaw = listOf(
            "#########.##########",
            "#..................#",
            "#.RRRRRR...RRRR....#",
            "#.RRRRRR...RRRR....#",
            "#.BWDWBB...BWDB....#",
            "#..................#",
            "#.,,,....MM......,,#",
            "#.,,,....MM......,,#",
            "#..................#",
            "#....HH......TT....#",
            "#....S.......S.....#",
            "#..................#",
            "#..RRRR......RRRR..#",
            "#..RRRR......RRRR..#",
            "#..BWWB......BWWB..#",
            "#..................#",
            "#..F,,,......,,,F..#",
            "####################"
        ),
        warps = listOf(
            Warp(9, 0, "route1", 9, 16),
            Warp(4, 4, "labor", 5, 7),
            Warp(13, 4, "zuhause", 4, 5)
        ),
        npcs = listOf(
            NpcDef("heim_wache", 9, 1, 0, "npc_m", NpcKind.TALK,
                lines = listOf(
                    "Halt! Im hohen Gras lauern wilde Monster.",
                    "Ohne einen eigenen Partner solltest du nicht hinaus.",
                    "Prof. Eibe im Labor sucht uebrigens einen Assistenten!"
                ),
                hiddenFlag = "starter"),
            NpcDef("heim_nurse", 7, 9, 2, "npc_f", NpcKind.TALK,
                lines = listOf(
                    "Willkommen an der Heilstation!",
                    "Stell dich vor das Geraet und benutze es - dein Team wird kostenlos geheilt."
                )),
            NpcDef("heim_sign1", 5, 10, 0, "sign", NpcKind.SIGN,
                lines = listOf("HEILSTATION - Dein Team wird hier kostenlos versorgt.")),
            NpcDef("heim_sign2", 13, 10, 0, "sign", NpcKind.SIGN,
                lines = listOf("MARKTSTAND - Hier gibt es Baelle, Traenke und mehr.")),
            NpcDef("heim_kid", 3, 7, 0, "npc_k", NpcKind.TALK,
                lines = listOf(
                    "Monster haben Typen wie Feuer, Wasser oder Pflanze!",
                    "Feuer ist stark gegen Pflanze, aber schwach gegen Wasser.",
                    "Merk dir das gut, sonst verlierst du staendig."
                )),
            NpcDef("heim_opa", 16, 15, 0, "npc_m", NpcKind.TALK,
                lines = listOf(
                    "Frueher bin ich selbst durch Auronia gereist.",
                    "Fuenf Arenen, fuenf Siegel - erst dann darf man zur Liga.",
                    "Und pass auf den Schattenorden auf. Die fuehren nichts Gutes im Schilde."
                ),
                afterFlag = "siegel5",
                afterLines = listOf(
                    "Alle fuenf Siegel! Du bist schon weiter gekommen als ich damals.",
                    "Im Schattental wartet der Orden auf dich. Gib acht, Kind."
                ))
        ),
        shop = shopA
    )

    private val zuhause = GameMap(
        id = "zuhause", name = "Dein Zuhause", indoor = true,
        rowsRaw = listOf(
            "wwwwwwwww",
            "wbboooobw",
            "wooooooow",
            "wo=ooo=ow",
            "woo===oow",
            "woo===oow",
            "wwwwDwwww"
        ),
        warps = listOf(Warp(4, 6, "heim", 13, 5)),
        npcs = listOf(
            NpcDef("mama", 2, 2, 0, "npc_f", NpcKind.HEALER,
                lines = listOf(
                    "Mama: Da bist du ja! Du siehst muede aus.",
                    "Mama: Komm her, ich paeppel dein Team wieder auf."
                ),
                afterLines = listOf("Mama: Pass gut auf dich auf, ja?"))
        )
    )

    private val labor = GameMap(
        id = "labor", name = "Labor von Prof. Eibe", indoor = true,
        rowsRaw = listOf(
            "wwwwwwwwwwwww",
            "wbbbbwwwbbbbw",
            "wooooooooooow",
            "woTTooooTToow",
            "wooo=====ooow",
            "woooo===oooow",
            "wooooo=ooooow",
            "wooooooooooow",
            "wwwwwDwwwwwww"
        ),
        warps = listOf(Warp(5, 8, "heim", 4, 5)),
        npcs = listOf(
            NpcDef("prof", 6, 3, 0, "npc_prof", NpcKind.TALK,
                lines = listOf(
                    "Prof. Eibe: Ah, endlich! Ich habe auf dich gewartet.",
                    "Prof. Eibe: Auronia ist voller Monster - und voller Raetsel.",
                    "Prof. Eibe: In den drei Kugeln dort wartet dein erster Partner.",
                    "Prof. Eibe: Waehle weise! Feuer, Wasser oder Pflanze."
                ),
                afterFlag = "starter",
                afterLines = listOf(
                    "Prof. Eibe: Besiege die fuenf Arenameister und stelle dich der Liga.",
                    "Prof. Eibe: Und behalte den Schattenorden im Auge. Sie suchen Titanox."
                )),
            NpcDef("ball_flamki", 5, 5, 0, "ball", NpcKind.TALK,
                lines = listOf("In dieser Kugel schlummert FLAMKI, ein Feuer-Monster.")),
            NpcDef("ball_aquino", 6, 5, 0, "ball", NpcKind.TALK,
                lines = listOf("In dieser Kugel schlummert AQUINO, ein Wasser-Monster.")),
            NpcDef("ball_sproutz", 7, 5, 0, "ball", NpcKind.TALK,
                lines = listOf("In dieser Kugel schlummert SPROUTZ, ein Pflanzen-Monster.")),
            NpcDef("assistent", 2, 2, 0, "npc_m", NpcKind.TALK,
                lines = listOf(
                    "Assistent: Jedes Monster hat einen oder zwei Typen.",
                    "Assistent: Attacken vom eigenen Typ richten 50% mehr Schaden an!",
                    "Das nennt man den Typ-Bonus."
                ))
        )
    )

    // ==================================================================
    //  Route 1
    // ==================================================================
    private val route1 = GameMap(
        id = "route1", name = "Route 1",
        rowsRaw = listOf(
            "#########.##########",
            "#........,.........#",
            "#.ggggg..,..ggggg..#",
            "#.ggggg..,..ggggg..#",
            "#........,.........#",
            "#..###...,....###..#",
            "#..###...,....###..#",
            "#........,,,,,,,,,.#",
            "#.gggg.......gggg..#",
            "#.gggg.......gggg..#",
            "#.gggg.......gggg..#",
            "#........,.........#",
            "#..F.....,.....F...#",
            "#........,.........#",
            "#.,,,,...,...,,,,,.#",
            "#........,.........#",
            "#........,.........#",
            "#########.##########"
        ),
        warps = listOf(
            Warp(9, 17, "heim", 9, 1),
            Warp(9, 0, "kupferstadt", 9, 16)
        ),
        npcs = listOf(
            NpcDef("r1_item", 3, 12, 0, "item", NpcKind.ITEM, itemId = "trank", itemCount = 2),
            NpcDef("r1_t1", 11, 7, 2, "npc_bug", NpcKind.TRAINER, sight = 3,
                lines = listOf("Kaefersammler Ben: Meine Kaefer sind kleine Kaempfer!"),
                afterLines = listOf("Kaefersammler Ben: Du hast wirklich Talent."),
                trainer = TrainerDef("r1_t1", "Kaefersammler Ben",
                    listOf(TrainerMon("raupix", 4), TrainerMon("piepsi", 5)), 180)),
            NpcDef("r1_t2", 7, 13, 3, "npc_f", NpcKind.TRAINER, sight = 3,
                lines = listOf("Wanderin Mia: Ein Kampf haelt fit!"),
                afterLines = listOf("Wanderin Mia: Der Weg nach Norden fuehrt nach Kupferstadt."),
                trainer = TrainerDef("r1_t2", "Wanderin Mia",
                    listOf(TrainerMon("ratzel", 5), TrainerMon("zappel", 5)), 200)),
            NpcDef("r1_sign", 10, 16, 0, "sign", NpcKind.SIGN,
                lines = listOf("ROUTE 1 - Nach Norden: Kupferstadt. Nach Sueden: Ahornfels."))
        ),
        encounters = listOf(
            Enc("ratzel", 2, 4, 30), Enc("piepsi", 2, 4, 30),
            Enc("raupix", 2, 4, 25), Enc("zappel", 3, 5, 15)
        ),
        encounterRate = 12
    )

    // ==================================================================
    //  Kupferstadt (Arena 1 - Kaefer)
    // ==================================================================
    private val kupferstadt = GameMap(
        id = "kupferstadt", name = "Kupferstadt",
        rowsRaw = listOf(
            "#########.##########",
            "#..................#",
            "#.RRRRRR....RRRR...#",
            "#.RRRRRR....RRRR...#",
            "#.BWDWBB....BWWB...#",
            "#..................#",
            "#....HH.....TT.....#",
            "#....S......S......#",
            "#..................#",
            "#...,,,,......,,,..#",
            "#..................#",
            "#..RRRRRRRR........#",
            "#..RRRRRRRR...RRRR.#",
            "#..BWWWDWWB...RRRR.#",
            "#.....M.......BWWB.#",
            "#..................#",
            "#........,.........#",
            "#########.##########"
        ),
        warps = listOf(
            Warp(9, 17, "route1", 9, 1),
            Warp(9, 0, "route2", 9, 16),
            Warp(7, 13, "arena1", 5, 11)
        ),
        npcs = listOf(
            NpcDef("ks_nurse", 7, 6, 2, "npc_f", NpcKind.TALK,
                lines = listOf("Die Heilstation ist immer kostenlos. Nutze sie oft!")),
            NpcDef("ks_sign1", 5, 7, 0, "sign", NpcKind.SIGN,
                lines = listOf("HEILSTATION - Dein Team wird hier kostenlos versorgt.")),
            NpcDef("ks_sign2", 12, 7, 0, "sign", NpcKind.SIGN,
                lines = listOf("MARKTSTAND - Waren fuer Reisende.")),
            NpcDef("ks_arenasign", 9, 14, 0, "sign", NpcKind.SIGN,
                lines = listOf("ARENA KUPFERSTADT - Meisterin Bella, die Herrin der Kaefer.")),
            NpcDef("ks_bergmann", 16, 9, 0, "npc_m", NpcKind.TALK,
                lines = listOf(
                    "Bergmann Kalle: Die Kristallhoehle im Norden ist stockdunkel.",
                    "Bergmann Kalle: Ohne Licht findet man da nie durch."
                ),
                afterLines = listOf(
                    "Bergmann Kalle: Du hast das Siegel? Respekt!",
                    "Bergmann Kalle: Hier, nimm meine Grubenlampe.",
                    "Du erhaeltst die GRUBENLAMPE!"
                ),
                requiresBadges = 1, givesItem = "lampe", setsFlag = "lampe_erhalten"),
            NpcDef("ks_kind", 3, 9, 0, "npc_k", NpcKind.TALK,
                lines = listOf(
                    "Kaefer sind stark gegen Pflanze - aber Feuer macht sie zu Asche!",
                    "Meisterin Bella hat noch nie gegen ein Feuer-Monster verloren. Angeblich."
                ))
        ),
        shop = shopB
    )

    private val arena1 = GameMap(
        id = "arena1", name = "Arena Kupferstadt", indoor = true, theme = "kaefer",
        rowsRaw = listOf(
            "wwwwwwwwwwwww",
            "wAAAAAAAAAAAw",
            "wAAAAAAAAAAAw",
            "wAAMAAAAAMAAw",
            "wAAAAAAAAAAAw",
            "wAAAAAAAAAAAw",
            "wAAMAAAAAMAAw",
            "wAAAAAAAAAAAw",
            "wAAAAAAAAAAAw",
            "wAAMAAAAAMAAw",
            "wAAAAAAAAAAAw",
            "wAAAAA=AAAAAw",
            "wwwwwDwwwwwww"
        ),
        warps = listOf(Warp(5, 12, "kupferstadt", 7, 14)),
        npcs = listOf(
            NpcDef("a1_boss", 6, 2, 0, "npc_boss1", NpcKind.TRAINER,
                lines = listOf(
                    "Meisterin Bella: Willkommen in meiner Arena!",
                    "Meisterin Bella: Kaefer sind zaeh, schnell und unterschaetzt.",
                    "Meisterin Bella: Zeig mir, ob du das erste Siegel verdienst!"
                ),
                afterLines = listOf(
                    "Meisterin Bella: Beeindruckend! Das KUPFER-SIEGEL gehoert dir.",
                    "Meisterin Bella: Nimm auch das hier - es macht deine Monster staerker."
                ),
                trainer = TrainerDef("boss1", "Meisterin Bella",
                    listOf(
                        TrainerMon("raupix", 8, listOf("tackle", "fadenschuss", "kaeferbiss")),
                        TrainerMon("kokonix", 10, listOf("kaeferbiss", "haertner", "fadenschuss")),
                        TrainerMon("glutwurm", 12, listOf("kaeferbiss", "glut", "fadenschuss"))
                    ),
                    money = 1200, boss = true, badge = "siegel1",
                    rewardItem = "sonderbonbon", rewardCount = 2,
                    winLine = "Meisterin Bella: Deine Monster vertrauen dir. Das entscheidet Kaempfe.")),
            NpcDef("a1_t1", 3, 7, 3, "npc_bug", NpcKind.TRAINER, sight = 2,
                lines = listOf("Arenaschueler Nino: An mir kommst du nicht vorbei!"),
                afterLines = listOf("Arenaschueler Nino: Bella wird dich schon aufhalten."),
                trainer = TrainerDef("a1_t1", "Arenaschueler Nino",
                    listOf(TrainerMon("raupix", 6), TrainerMon("kokonix", 7)), 320)),
            NpcDef("a1_t2", 9, 7, 2, "npc_bug", NpcKind.TRAINER, sight = 2,
                lines = listOf("Arenaschuelerin Ida: Kaefer sind die Zukunft!"),
                afterLines = listOf("Arenaschuelerin Ida: Nicht schlecht ..."),
                trainer = TrainerDef("a1_t2", "Arenaschuelerin Ida",
                    listOf(TrainerMon("glutwurm", 8)), 300))
        )
    )

    // ==================================================================
    //  Route 2 + Kristallhoehle
    // ==================================================================
    private val route2 = GameMap(
        id = "route2", name = "Route 2",
        rowsRaw = listOf(
            "####################",
            "#^^^^^^^^D^^^^^^^^^#",
            "#........,.........#",
            "#..gggg..,..ggggg..#",
            "#..gggg..,..ggggg..#",
            "#........,.........#",
            "#..###...,......##.#",
            "#..###...,......##.#",
            "#....,,,,,,,,,,....#",
            "#....,.........,...#",
            "#.ggg,.........ggg.#",
            "#.ggg,.........ggg.#",
            "#.ggg,.........ggg.#",
            "#....,,,,,,,,,,,...#",
            "#........,.........#",
            "#..F.....,......F..#",
            "#........,.........#",
            "#########.##########"
        ),
        warps = listOf(
            Warp(9, 17, "kupferstadt", 9, 1),
            Warp(9, 1, "hoehle", 11, 17, requiresFlag = "lampe_erhalten")
        ),
        npcs = listOf(
            NpcDef("r2_item", 17, 9, 0, "item", NpcKind.ITEM, itemId = "superball", itemCount = 3),
            NpcDef("r2_item2", 2, 15, 0, "item", NpcKind.ITEM, itemId = "supertrank"),
            NpcDef("r2_sign", 10, 2, 0, "sign", NpcKind.SIGN,
                lines = listOf("KRISTALLHOEHLE - Betreten nur mit Licht! Sehr dunkel.")),
            NpcDef("r2_t1", 12, 8, 2, "npc_m", NpcKind.TRAINER, sight = 4,
                lines = listOf("Wanderer Rolf: Der Weg nach Norden ist steinig!"),
                afterLines = listOf("Wanderer Rolf: In der Hoehle wimmelt es von Gesteins-Monstern."),
                trainer = TrainerDef("r2_t1", "Wanderer Rolf",
                    listOf(TrainerMon("steinkopf", 9), TrainerMon("ratzel", 9)), 420)),
            NpcDef("r2_t2", 5, 12, 3, "npc_dark", NpcKind.TRAINER, sight = 3,
                lines = listOf(
                    "Schattenorden-Rekrut: Diese Gegend gehoert jetzt uns!",
                    "Schattenorden-Rekrut: Verschwinde - oder kaempfe!"
                ),
                afterLines = listOf("Schattenorden-Rekrut: Der Orden wird Titanox erwecken. Du kannst uns nicht aufhalten!"),
                trainer = TrainerDef("r2_t2", "Schattenorden-Rekrut",
                    listOf(TrainerMon("giftling", 10), TrainerMon("schemen", 11)), 500))
        ),
        encounters = listOf(
            Enc("ratzel", 6, 9, 20), Enc("piepsi", 6, 9, 20), Enc("giftling", 7, 9, 20),
            Enc("knospel", 7, 10, 15), Enc("glutwurm", 8, 10, 10), Enc("zappel", 7, 9, 15)
        )
    )

    private val hoehle = GameMap(
        id = "hoehle", name = "Kristallhoehle", dark = true, theme = "hoehle",
        rowsRaw = listOf(
            "^^^^^^^^^^^D^^^^^^^^^^^^",
            "^CCCCCCCCCCCCCCCC^^^^^^^",
            "^CCC^^^^CCCC^^^^CCCCCC^^",
            "^CCC^^^^CCCC^^^^^^^^CC^^",
            "^CCCCCCCCCCCCCCCC^^^CC^^",
            "^^^^^^CC^^^^^^^^C^^^CC^^",
            "^XCCCCCC^^^^^^^^CCCCCC^^",
            "^CCCCCCCCCCCC^^^^^^^^C^^",
            "^CC^^^^^^^^CC^^^^^^^^C^^",
            "^CC^^^^^^^^CCCCCCCCCCC^^",
            "^CCCCCC^^^^^^^^^^^^^^^^^",
            "^^^^^CC^^^^^^^^^^CCCCC^^",
            "^CCCCCCCCCCCCCCCCCCCCC^^",
            "^CC^^^^^^^^^^^^^^^^^^^^^",
            "^CC^^^^^^^^^^^^^^^^^^^^^",
            "^CCCCCCCCCCC^^^^^^^^^^^^",
            "^^^^^^^^^^CC^^^^^^^^^^^^",
            "^^^^^^^^^^CC^^^^^^^^^^^^",
            "^^^^^^^^^^CD^^^^^^^^^^^^",
            "^^^^^^^^^^^^^^^^^^^^^^^^"
        ),
        warps = listOf(
            Warp(11, 18, "route2", 9, 2),
            Warp(11, 0, "wellenstadt", 9, 16)
        ),
        npcs = listOf(
            NpcDef("h_item1", 2, 13, 0, "item", NpcKind.ITEM, itemId = "hyperball", itemCount = 2),
            NpcDef("h_item2", 21, 2, 0, "item", NpcKind.ITEM, itemId = "sonderbonbon"),
            NpcDef("h_item3", 6, 6, 0, "item", NpcKind.ITEM, itemId = "elixier"),
            NpcDef("h_t1", 11, 7, 0, "npc_m", NpcKind.TRAINER, sight = 3,
                lines = listOf("Hoehlenforscher Urs: Wer wagt sich in mein Revier?"),
                afterLines = listOf("Hoehlenforscher Urs: Nordwaerts kommst du nach Wellenstadt."),
                trainer = TrainerDef("h_t1", "Hoehlenforscher Urs",
                    listOf(TrainerMon("steinkopf", 12), TrainerMon("schemen", 12)), 620)),
            NpcDef("h_t2", 18, 9, 2, "npc_dark", NpcKind.TRAINER, sight = 3,
                lines = listOf("Schattenorden-Rekrut: Wir suchen hier Kristalle fuer den Orden!"),
                afterLines = listOf("Schattenorden-Rekrut: Morgana wird das nicht gefallen ..."),
                trainer = TrainerDef("h_t2", "Schattenorden-Rekrut",
                    listOf(TrainerMon("giftling", 13), TrainerMon("schemen", 13)), 680))
        ),
        encounters = listOf(
            Enc("steinkopf", 9, 13, 30), Enc("zappel", 9, 12, 20), Enc("schemen", 10, 13, 20),
            Enc("giftling", 9, 12, 20), Enc("felsberg", 13, 14, 5), Enc("frostli", 11, 13, 5)
        ),
        encounterRate = 14
    )

    // ==================================================================
    //  Wellenstadt (Arena 2 - Wasser)
    // ==================================================================
    private val wellenstadt = GameMap(
        id = "wellenstadt", name = "Wellenstadt",
        rowsRaw = listOf(
            "####################",
            "#~~~~~~~~~~~~~~~~~~#",
            "#~~~~~~~~~~~~~~~~~~#",
            "#__________________#",
            "#.RRRRRR...RRRRRR..#",
            "#.RRRRRR...RRRRRR..#",
            "#.BWWDWB...BWWDWB..#",
            "#..................#",
            "#....HH.....TT.....#",
            "#....S......S......#",
            "#..................#",
            "#...,,,,,,,,,,,,....",
            "#..................#",
            "#..RRRRRRRR........#",
            "#..RRRRRRRR...RRRR.#",
            "#..BWWWDWWB...BWWB.#",
            "#........,.........#",
            "#########D##########"
        ),
        warps = listOf(
            Warp(9, 17, "hoehle", 11, 1),
            Warp(19, 11, "route3", 1, 9),
            Warp(7, 15, "arena2", 5, 11)
        ),
        npcs = listOf(
            NpcDef("ws_nurse", 7, 8, 2, "npc_f", NpcKind.TALK,
                lines = listOf("Vergiss nicht zu heilen, bevor du aufs Meer hinausschaust!")),
            NpcDef("ws_sign1", 5, 9, 0, "sign", NpcKind.SIGN,
                lines = listOf("HEILSTATION - Kostenlose Pflege fuer dein Team.")),
            NpcDef("ws_sign2", 12, 9, 0, "sign", NpcKind.SIGN,
                lines = listOf("MARKTSTAND VON WELLENSTADT")),
            NpcDef("ws_angler", 5, 3, 0, "npc_m", NpcKind.TALK,
                lines = listOf(
                    "Angler Jonas: Das Meer ist voller Monster, die man nur mit einer Angel erwischt.",
                    "Angler Jonas: Weisst du was? Nimm meine Zweitangel!",
                    "Du erhaeltst die ANGEL! Benutze sie im Beutel am Wasser."
                ),
                afterLines = listOf("Angler Jonas: Stell dich ans Ufer und wirf die Angel aus!"),
                givesItem = "angel", setsFlag = "angel_erhalten"),
            NpcDef("ws_arenasign", 5, 16, 0, "sign", NpcKind.SIGN,
                lines = listOf("ARENA WELLENSTADT - Meisterin Nerina, Stimme der Gezeiten.")),
            NpcDef("ws_item", 17, 3, 0, "item", NpcKind.ITEM, itemId = "wasserstein")
        ),
        fishing = listOf(
            Enc("seeklinge", 10, 15, 35), Enc("schlammo", 10, 14, 25),
            Enc("korallis", 12, 16, 25), Enc("aquino", 12, 15, 15)
        ),
        shop = shopC
    )

    private val arena2 = GameMap(
        id = "arena2", name = "Arena Wellenstadt", indoor = true, theme = "wasser",
        rowsRaw = listOf(
            "wwwwwwwwwwwww",
            "wAAAAAAAAAAAw",
            "wAA~~~A~~~AAw",
            "wAA~~~A~~~AAw",
            "wAAAAAAAAAAAw",
            "wA~~~AAA~~~Aw",
            "wA~~~AAA~~~Aw",
            "wAAAAAAAAAAAw",
            "wAA~~~A~~~AAw",
            "wAA~~~A~~~AAw",
            "wAAAAAAAAAAAw",
            "wAAAAA=AAAAAw",
            "wwwwwDwwwwwww"
        ),
        warps = listOf(Warp(5, 12, "wellenstadt", 7, 16)),
        npcs = listOf(
            NpcDef("a2_boss", 6, 1, 0, "npc_boss2", NpcKind.TRAINER,
                lines = listOf(
                    "Meisterin Nerina: Das Meer kennt kein Erbarmen.",
                    "Meisterin Nerina: Meine Wellen haben schon viele Trainer verschluckt.",
                    "Meisterin Nerina: Mal sehen, ob du schwimmen kannst!"
                ),
                afterLines = listOf(
                    "Meisterin Nerina: Du reitest die Welle, statt gegen sie zu kaempfen.",
                    "Meisterin Nerina: Das WELLEN-SIEGEL ist dein!"
                ),
                trainer = TrainerDef("boss2", "Meisterin Nerina",
                    listOf(
                        TrainerMon("korallis", 16, listOf("blubbstrahl", "steinwurf", "haertner")),
                        TrainerMon("seeklinge", 17, listOf("aquahaubitze", "nassschweif", "agilitaet")),
                        TrainerMon("wavox", 20, listOf("surfer", "frostatem", "blubbstrahl", "haertner"))
                    ),
                    money = 2400, boss = true, badge = "siegel2",
                    rewardItem = "hyperball", rewardCount = 5,
                    winLine = "Meisterin Nerina: Nimm das Siegel - du hast es dir erkaempft.")),
            NpcDef("a2_t1", 3, 7, 3, "npc_f", NpcKind.TRAINER, sight = 2,
                lines = listOf("Schwimmerin Tara: Nass wirst du sowieso!"),
                afterLines = listOf("Schwimmerin Tara: Puh, du bist gut."),
                trainer = TrainerDef("a2_t1", "Schwimmerin Tara",
                    listOf(TrainerMon("aquino", 16), TrainerMon("schlammo", 16)), 700)),
            NpcDef("a2_t2", 9, 7, 2, "npc_m", NpcKind.TRAINER, sight = 2,
                lines = listOf("Matrose Kurt: Immer schoen auf Kurs bleiben!"),
                afterLines = listOf("Matrose Kurt: Nerina wartet oben auf dich."),
                trainer = TrainerDef("a2_t2", "Matrose Kurt",
                    listOf(TrainerMon("seeklinge", 17), TrainerMon("korallis", 17)), 720))
        )
    )

    // ==================================================================
    //  Route 3
    // ==================================================================
    private val route3 = GameMap(
        id = "route3", name = "Route 3",
        rowsRaw = listOf(
            "####################",
            "#..gggg....gggg....#",
            "#..gggg....gggg....#",
            "#..................#",
            "#~~~~~~....###.....#",
            "#~~~~~~....###.....#",
            "#~~~~~_............#",
            "#_____.............#",
            "#..................#",
            "....................",
            "#..................#",
            "#....gggg...gggg...#",
            "#....gggg...gggg...#",
            "#....gggg...gggg...#",
            "#..................#",
            "#..###.......###...#",
            "#..###.......###...#",
            "####################"
        ),
        warps = listOf(
            Warp(0, 9, "wellenstadt", 18, 11),
            Warp(19, 9, "aschenfeld", 1, 9)
        ),
        npcs = listOf(
            NpcDef("r3_item", 15, 2, 0, "item", NpcKind.ITEM, itemId = "beleber"),
            NpcDef("r3_item2", 6, 16, 0, "item", NpcKind.ITEM, itemId = "xangriff", itemCount = 2),
            NpcDef("r3_t1", 12, 9, 2, "npc_m", NpcKind.TRAINER, sight = 4,
                lines = listOf("Wanderer Sepp: Der Weg nach Aschenfeld ist heiss!"),
                afterLines = listOf("Wanderer Sepp: Nimm genug Wasser-Monster mit."),
                trainer = TrainerDef("r3_t1", "Wanderer Sepp",
                    listOf(TrainerMon("kaktrix", 16), TrainerMon("steinkopf", 16)), 900)),
            NpcDef("r3_t2", 8, 13, 1, "npc_f", NpcKind.TRAINER, sight = 3,
                lines = listOf("Kraeuterfrau Doro: Meine Pflanzen beissen zurueck!"),
                afterLines = listOf("Kraeuterfrau Doro: Bei Aschenfeld waechst kaum noch etwas."),
                trainer = TrainerDef("r3_t2", "Kraeuterfrau Doro",
                    listOf(TrainerMon("knospel", 17), TrainerMon("blattor", 18)), 950)),
            NpcDef("r3_sign", 10, 8, 0, "sign", NpcKind.SIGN,
                lines = listOf("ROUTE 3 - Osten: Aschenfeld. Westen: Wellenstadt."))
        ),
        encounters = listOf(
            Enc("piepsi", 13, 16, 20), Enc("kaktrix", 13, 16, 20), Enc("giftling", 13, 16, 15),
            Enc("schlammo", 13, 16, 15), Enc("ratzel", 13, 16, 15), Enc("rattmar", 16, 18, 8),
            Enc("knospel", 14, 17, 12)
        ),
        fishing = listOf(
            Enc("seeklinge", 14, 18, 40), Enc("schlammo", 14, 18, 30), Enc("korallis", 15, 19, 30)
        )
    )

    // ==================================================================
    //  Aschenfeld (Arena 3 - Feuer)
    // ==================================================================
    private val aschenfeld = GameMap(
        id = "aschenfeld", name = "Aschenfeld",
        rowsRaw = listOf(
            "#########.##########",
            "#VV....VV..VV....VV#",
            "#V..............V..#",
            "#..RRRRRR..RRRR....#",
            "#..RRRRRR..RRRR....#",
            "#..BWWDWB..BWDB....#",
            "#..................#",
            "#....HH.....TT.....#",
            "#....S......S......#",
            "...................#",
            "#..................#",
            "#..LL.....RRRRRR...#",
            "#..LL.....RRRRRR...#",
            "#.........BWWDWB...#",
            "#..................#",
            "#..VV.......VV.....#",
            "#..................#",
            "####################"
        ),
        warps = listOf(
            Warp(0, 9, "route3", 18, 9),
            Warp(9, 0, "route4", 9, 16),
            Warp(13, 13, "arena3", 5, 11)
        ),
        npcs = listOf(
            NpcDef("as_nurse", 7, 7, 2, "npc_f", NpcKind.TALK,
                lines = listOf("Die Hitze setzt den Monstern zu. Heile sie regelmaessig!")),
            NpcDef("as_sign1", 5, 8, 0, "sign", NpcKind.SIGN,
                lines = listOf("HEILSTATION - Kostenlose Pflege fuer dein Team.")),
            NpcDef("as_sign2", 12, 8, 0, "sign", NpcKind.SIGN,
                lines = listOf("MARKTSTAND VON ASCHENFELD")),
            NpcDef("as_arenasign", 11, 14, 0, "sign", NpcKind.SIGN,
                lines = listOf("ARENA ASCHENFELD - Meister Ignaz, Herr der Glut.")),
            NpcDef("as_item", 3, 16, 0, "item", NpcKind.ITEM, itemId = "feuerstein"),
            NpcDef("as_forscher", 16, 6, 0, "npc_prof", NpcKind.TALK,
                lines = listOf(
                    "Vulkanforscherin: Unter Aschenfeld schlaeft ein alter Krater.",
                    "Vulkanforscherin: Manche Monster entwickeln sich nur mit einem Feuerstein.",
                    "Vulkanforscherin: Steine findest du in der Umgebung oder auf grossen Maerkten."
                ))
        ),
        shop = shopD
    )

    private val arena3 = GameMap(
        id = "arena3", name = "Arena Aschenfeld", indoor = true, theme = "feuer",
        rowsRaw = listOf(
            "wwwwwwwwwwwww",
            "wAAAAAAAAAAAw",
            "wAALAAAAALAAw",
            "wAAAAAAAAAAAw",
            "wAAAALLLAAAAw",
            "wAAAAAAAAAAAw",
            "wALAAAAAAALAw",
            "wAAAAAAAAAAAw",
            "wAAAALLLAAAAw",
            "wAAAAAAAAAAAw",
            "wAALAAAAALAAw",
            "wAAAAA=AAAAAw",
            "wwwwwDwwwwwww"
        ),
        warps = listOf(Warp(5, 12, "aschenfeld", 13, 14)),
        npcs = listOf(
            NpcDef("a3_boss", 6, 1, 0, "npc_boss3", NpcKind.TRAINER,
                lines = listOf(
                    "Meister Ignaz: Spuerst du die Hitze? Das ist mein Herzschlag!",
                    "Meister Ignaz: Wer hier besteht, brennt fuer den Kampf.",
                    "Meister Ignaz: Los! Zeig mir dein Feuer!"
                ),
                afterLines = listOf(
                    "Meister Ignaz: HAHA! Du hast mich kalt erwischt.",
                    "Meister Ignaz: Das GLUT-SIEGEL ist deins!"
                ),
                trainer = TrainerDef("boss3", "Meister Ignaz",
                    listOf(
                        TrainerMon("flufflamm", 25, listOf("flammenwurf", "hitzeschutz", "heuler")),
                        TrainerMon("glutwurm", 26, listOf("flammenwurf", "sichelschlag", "kaefergebrumm")),
                        TrainerMon("flammor", 29, listOf("flammenwurf", "feuerzahn", "schlitzer", "ruckzuck"))
                    ),
                    money = 3600, boss = true, badge = "siegel3",
                    rewardItem = "toptrank", rewardCount = 3,
                    winLine = "Meister Ignaz: Feuer respektiert nur staerkeres Feuer.")),
            NpcDef("a3_t1", 3, 7, 3, "npc_m", NpcKind.TRAINER, sight = 2,
                lines = listOf("Feuerschlucker Rico: Zu heiss fuer dich?"),
                afterLines = listOf("Feuerschlucker Rico: Autsch, das war stark."),
                trainer = TrainerDef("a3_t1", "Feuerschlucker Rico",
                    listOf(TrainerMon("flamki", 22), TrainerMon("glutwurm", 23)), 1100)),
            NpcDef("a3_t2", 9, 7, 2, "npc_f", NpcKind.TRAINER, sight = 2,
                lines = listOf("Schmiedin Elke: Im Feuer wird Stahl geboren!"),
                afterLines = listOf("Schmiedin Elke: Du haeltst was aus."),
                trainer = TrainerDef("a3_t2", "Schmiedin Elke",
                    listOf(TrainerMon("steinkopf", 22), TrainerMon("flufflamm", 23)), 1150))
        )
    )

    // ==================================================================
    //  Route 4 - Nebelwald
    // ==================================================================
    private val route4 = GameMap(
        id = "route4", name = "Nebelwald",
        rowsRaw = listOf(
            "#########.##########",
            "#IIII...,,,....IIII#",
            "#IIII...,,,....IIII#",
            "#.......,,,........#",
            "#.gggg..,,,..gggg..#",
            "#.gggg..,,,..gggg..#",
            "#.......,,,........#",
            "#.###...,,,....###.#",
            "#.###...,,,....###.#",
            "#.......,,,........#",
            "#..gggggg..gggggg..#",
            "#..gggggg..gggggg..#",
            "#.......,,,........#",
            "#.###...,,,....###.#",
            "#.###...,,,....###.#",
            "#.......,,,........#",
            "#.......,,,........#",
            "#########.##########"
        ),
        warps = listOf(
            Warp(9, 17, "aschenfeld", 9, 1),
            Warp(9, 0, "frostheim", 9, 16)
        ),
        npcs = listOf(
            NpcDef("r4_item", 2, 10, 0, "item", NpcKind.ITEM, itemId = "hypertrank", itemCount = 2),
            NpcDef("r4_item2", 17, 4, 0, "item", NpcKind.ITEM, itemId = "blattstein"),
            NpcDef("r4_t1", 8, 6, 0, "npc_bug", NpcKind.TRAINER, sight = 4,
                lines = listOf("Waldlaeufer Finn: Im Nebel verirren sich die meisten!"),
                afterLines = listOf("Waldlaeufer Finn: Nordwaerts wird es eisig."),
                trainer = TrainerDef("r4_t1", "Waldlaeufer Finn",
                    listOf(TrainerMon("mothara", 22), TrainerMon("knospel", 23)), 1300)),
            NpcDef("r4_t2", 10, 12, 1, "npc_m", NpcKind.TRAINER, sight = 4,
                lines = listOf("Kaempfer Bodo: Meine Fauste sind mein Werkzeug!"),
                afterLines = listOf("Kaempfer Bodo: Guter Kampf!"),
                trainer = TrainerDef("r4_t2", "Kaempfer Bodo",
                    listOf(TrainerMon("boxor", 23), TrainerMon("kampfax", 25)), 1450)),
            NpcDef("r4_t3", 14, 16, 2, "npc_dark", NpcKind.TRAINER, sight = 3,
                lines = listOf("Schattenorden-Kaempfer: Der Orden duldet keine Zeugen!"),
                afterLines = listOf("Schattenorden-Kaempfer: Morgana ... verzeih mir."),
                trainer = TrainerDef("r4_t3", "Schattenorden-Kaempfer",
                    listOf(TrainerMon("toxarr", 24), TrainerMon("nachtgeist", 25)), 1600))
        ),
        encounters = listOf(
            Enc("kokonix", 17, 20, 15), Enc("mothara", 19, 22, 10), Enc("schemen", 18, 21, 20),
            Enc("knospel", 18, 21, 20), Enc("boxor", 18, 21, 15), Enc("frostli", 19, 22, 15),
            Enc("rattmar", 19, 22, 5)
        )
    )

    // ==================================================================
    //  Frostheim (Arena 4 - Eis)
    // ==================================================================
    private val frostheim = GameMap(
        id = "frostheim", name = "Frostheim",
        rowsRaw = listOf(
            "#########.##########",
            "#..................#",
            "#.RRRRRR....RRRR...#",
            "#.RRRRRR....RRRR...#",
            "#.BWWDWB....BWWB...#",
            "#..................#",
            "#....HH.....TT.....#",
            "#....S......S......#",
            "#..IIIIIIIIIIII....#",
            "#..IIIIIIIIIIII....#",
            "#..................#",
            "#..RRRRRRRR........#",
            "#..RRRRRRRR...RRRR.#",
            "#..BWWWDWWB...BWWB.#",
            "#..................#",
            "#....,,......,,....#",
            "#........,.........#",
            "#########.##########"
        ),
        warps = listOf(
            Warp(9, 17, "route4", 9, 1),
            Warp(9, 0, "route5", 9, 16),
            Warp(7, 13, "arena4", 5, 11)
        ),
        npcs = listOf(
            NpcDef("fh_nurse", 7, 6, 2, "npc_f", NpcKind.TALK,
                lines = listOf("Bei der Kaelte braucht dein Team oft eine Pause. Die Heilstation ist gratis!")),
            NpcDef("fh_sign1", 5, 7, 0, "sign", NpcKind.SIGN,
                lines = listOf("HEILSTATION - Kostenlose Pflege fuer dein Team.")),
            NpcDef("fh_sign2", 12, 7, 0, "sign", NpcKind.SIGN,
                lines = listOf("MARKTSTAND VON FROSTHEIM")),
            NpcDef("fh_arenasign", 5, 14, 0, "sign", NpcKind.SIGN,
                lines = listOf("ARENA FROSTHEIM - Meisterin Frostina, Atem des Winters.")),
            NpcDef("fh_kind", 16, 10, 0, "npc_k", NpcKind.TALK,
                lines = listOf(
                    "Auf dem Eis kann man nicht bremsen!",
                    "Man rutscht immer weiter, bis etwas im Weg steht."
                )),
            NpcDef("fh_item", 3, 15, 0, "item", NpcKind.ITEM, itemId = "topbeleber")
        ),
        shop = shopE
    )

    private val arena4 = GameMap(
        id = "arena4", name = "Arena Frostheim", indoor = true, theme = "eis",
        rowsRaw = listOf(
            "wwwwwwwwwwwww",
            "wAAAAAAAAAAAw",
            "wAAIIIAIIIAAw",
            "wAAIIIAIIIAAw",
            "wAAAAAAAAAAAw",
            "wAIIIIAIIIIAw",
            "wAIIIIAIIIIAw",
            "wAAAAAAAAAAAw",
            "wAAIIIAIIIAAw",
            "wAAIIIAIIIAAw",
            "wAAAAAAAAAAAw",
            "wAAAAA=AAAAAw",
            "wwwwwDwwwwwww"
        ),
        warps = listOf(Warp(5, 12, "frostheim", 7, 14)),
        npcs = listOf(
            NpcDef("a4_boss", 6, 1, 0, "npc_boss4", NpcKind.TRAINER,
                lines = listOf(
                    "Meisterin Frostina: Kaelte ist ehrlich. Sie kennt keine Ausreden.",
                    "Meisterin Frostina: Meine Monster frieren deine Traeume ein.",
                    "Meisterin Frostina: Beginnen wir."
                ),
                afterLines = listOf(
                    "Meisterin Frostina: Dein Wille ist waermer als jedes Feuer.",
                    "Meisterin Frostina: Nimm das FROST-SIEGEL."
                ),
                trainer = TrainerDef("boss4", "Meisterin Frostina",
                    listOf(
                        TrainerMon("frostli", 29, listOf("eishieb", "frostschleier", "haertner")),
                        TrainerMon("seeklinge", 29, listOf("aquahaubitze", "frostatem", "agilitaet")),
                        TrainerMon("glaciar", 32, listOf("eisstrahl", "steinkante", "frostschleier", "eishieb"))
                    ),
                    money = 5000, boss = true, badge = "siegel4",
                    rewardItem = "elixier", rewardCount = 3,
                    winLine = "Meisterin Frostina: Der Winter beugt sich vor dir.")),
            NpcDef("a4_t1", 3, 7, 3, "npc_f", NpcKind.TRAINER, sight = 2,
                lines = listOf("Skifahrerin Lena: Rutsch nicht aus!"),
                afterLines = listOf("Skifahrerin Lena: Du haeltst dich gut auf dem Eis."),
                trainer = TrainerDef("a4_t1", "Skifahrerin Lena",
                    listOf(TrainerMon("frostli", 27), TrainerMon("piepsi", 27)), 1800)),
            NpcDef("a4_t2", 9, 7, 2, "npc_m", NpcKind.TRAINER, sight = 2,
                lines = listOf("Bergfuehrer Toni: Hier oben zaehlt Ausdauer!"),
                afterLines = listOf("Bergfuehrer Toni: Frostina wird dich fordern."),
                trainer = TrainerDef("a4_t2", "Bergfuehrer Toni",
                    listOf(TrainerMon("felsberg", 28), TrainerMon("glaciar", 29)), 1900))
        )
    )

    // ==================================================================
    //  Route 5
    // ==================================================================
    private val route5 = GameMap(
        id = "route5", name = "Route 5",
        rowsRaw = listOf(
            "#########.##########",
            "#..................#",
            "#.^^^^..gggg..^^^^.#",
            "#.^^^^..gggg..^^^^.#",
            "#.......gggg.......#",
            "#..gggg......gggg..#",
            "#..gggg......gggg..#",
            "#.......,,,,.......#",
            "#.^^^..........^^^.#",
            "#.^^^..........^^^.#",
            "#.......,,,,.......#",
            "#..gggg......gggg..#",
            "#..gggg......gggg..#",
            "#.......,,,,.......#",
            "#.^^^^........^^^^.#",
            "#..................#",
            "#........,.........#",
            "#########.##########"
        ),
        warps = listOf(
            Warp(9, 17, "frostheim", 9, 1),
            Warp(9, 0, "sturmfeste", 9, 16)
        ),
        npcs = listOf(
            NpcDef("r5_item", 2, 15, 0, "item", NpcKind.ITEM, itemId = "donnerstein"),
            NpcDef("r5_item2", 17, 1, 0, "item", NpcKind.ITEM, itemId = "toptrank", itemCount = 2),
            NpcDef("r5_t1", 9, 7, 0, "npc_m", NpcKind.TRAINER, sight = 4,
                lines = listOf("Gewitterjaeger Nils: Der Sturm ruft mich!"),
                afterLines = listOf("Gewitterjaeger Nils: Ueber uns liegt die Sturmfeste."),
                trainer = TrainerDef("r5_t1", "Gewitterjaeger Nils",
                    listOf(TrainerMon("funkling", 28), TrainerMon("voltrax", 30)), 2200)),
            NpcDef("r5_t2", 11, 13, 1, "npc_f", NpcKind.TRAINER, sight = 4,
                lines = listOf("Wanderin Silke: Der Aufstieg ist steil - genau wie ich!"),
                afterLines = listOf("Wanderin Silke: Respekt, Trainer."),
                trainer = TrainerDef("r5_t2", "Wanderin Silke",
                    listOf(TrainerMon("kaktrix", 29), TrainerMon("rattmar", 30)), 2300)),
            NpcDef("r5_t3", 5, 4, 3, "npc_dark", NpcKind.TRAINER, sight = 3,
                lines = listOf("Schattenorden-Kaempferin: Du kommst dem Orden zu nahe!"),
                afterLines = listOf("Schattenorden-Kaempferin: Im Schattental wirst du untergehen."),
                trainer = TrainerDef("r5_t3", "Schattenorden-Kaempferin",
                    listOf(TrainerMon("mothara", 29), TrainerMon("toxarr", 31)), 2500))
        ),
        encounters = listOf(
            Enc("funkling", 24, 28, 25), Enc("zappel", 24, 27, 15), Enc("voltrax", 27, 29, 8),
            Enc("boxor", 24, 27, 15), Enc("kaktrix", 24, 27, 17), Enc("sturmfalk", 26, 29, 10),
            Enc("felsberg", 26, 29, 10)
        )
    )

    // ==================================================================
    //  Sturmfeste (Arena 5 - Elektro)
    // ==================================================================
    private val sturmfeste = GameMap(
        id = "sturmfeste", name = "Sturmfeste",
        rowsRaw = listOf(
            "####################",
            "#..................#",
            "#.RRRRRR...RRRRRR..#",
            "#.BWWDWB...BWWDWB..#",
            "#..................#",
            "#....HH.....TT.....#",
            "#....S......S......#",
            "#..................#",
            "#..MM..........MM..#",
            "#...................",
            "#..................#",
            "#..RRRRRRRR........#",
            "#..RRRRRRRR........#",
            "#..BWWWDWWB........#",
            "#..................#",
            "#....,,....,,,.....#",
            "#........,.........#",
            "#########.##########"
        ),
        warps = listOf(
            Warp(9, 17, "route5", 9, 1),
            Warp(19, 9, "schattental", 1, 9),
            Warp(7, 13, "arena5", 5, 11)
        ),
        npcs = listOf(
            NpcDef("sf_nurse", 7, 5, 2, "npc_f", NpcKind.TALK,
                lines = listOf("Willkommen in der Sturmfeste. Die Heilstation ist gleich hier.")),
            NpcDef("sf_sign1", 5, 6, 0, "sign", NpcKind.SIGN,
                lines = listOf("HEILSTATION - Kostenlose Pflege fuer dein Team.")),
            NpcDef("sf_sign2", 12, 6, 0, "sign", NpcKind.SIGN,
                lines = listOf("MARKTSTAND DER STURMFESTE")),
            NpcDef("sf_arenasign", 5, 14, 0, "sign", NpcKind.SIGN,
                lines = listOf("ARENA STURMFESTE - Meister Volter, Zorn der Blitze.")),
            NpcDef("sf_wache", 18, 9, 2, "npc_m", NpcKind.TALK,
                lines = listOf(
                    "Wache: Hinter mir liegt das Schattental.",
                    "Wache: Dort haust der Schattenorden. Ohne alle fuenf Siegel lasse ich niemanden durch!"
                ),
                hiddenFlag = "siegel5"),
            NpcDef("sf_bote", 14, 8, 0, "npc_f", NpcKind.TALK,
                lines = listOf(
                    "Botin: Fuenf Siegel! Dann bist du unsere letzte Hoffnung.",
                    "Botin: Der Orden will Titanox erwecken. Halte sie im Schattental auf!"
                ),
                requiresFlag = "siegel5"),
            NpcDef("sf_item", 16, 15, 0, "item", NpcKind.ITEM, itemId = "meisterball")
        ),
        shop = shopE
    )

    private val arena5 = GameMap(
        id = "arena5", name = "Arena Sturmfeste", indoor = true, theme = "elektro",
        rowsRaw = listOf(
            "wwwwwwwwwwwww",
            "wAAAAAAAAAAAw",
            "wAAMAAAAAMAAw",
            "wAAAAAAAAAAAw",
            "wAAAAMMMAAAAw",
            "wAAAAAAAAAAAw",
            "wAMAAAAAAAMAw",
            "wAAAAAAAAAAAw",
            "wAAAAMMMAAAAw",
            "wAAAAAAAAAAAw",
            "wAAMAAAAAMAAw",
            "wAAAAA=AAAAAw",
            "wwwwwDwwwwwww"
        ),
        warps = listOf(Warp(5, 12, "sturmfeste", 7, 14)),
        npcs = listOf(
            NpcDef("a5_boss", 6, 1, 0, "npc_boss5", NpcKind.TRAINER,
                lines = listOf(
                    "Meister Volter: Du hast dich bis hier hochgekaempft. Gut!",
                    "Meister Volter: Blitze warten nicht. Und ich auch nicht.",
                    "Meister Volter: Das letzte Siegel wirst du dir teuer erkaufen!"
                ),
                afterLines = listOf(
                    "Meister Volter: Unglaublich. Du schlaegst schneller als der Donner.",
                    "Meister Volter: Das STURM-SIEGEL - dein fuenftes! Nun steht dir die Liga offen."
                ),
                trainer = TrainerDef("boss5", "Meister Volter",
                    listOf(
                        TrainerMon("funkling", 35, listOf("donnerblitz", "donnerwelle", "agilitaet")),
                        TrainerMon("voltrax", 37, listOf("donnerblitz", "ladungsstoss", "agilitaet", "funkenflug")),
                        TrainerMon("gigavolt", 40, listOf("donner", "steinkante", "erdbeben", "donnerblitz"))
                    ),
                    money = 7000, boss = true, badge = "siegel5",
                    rewardItem = "sonderbonbon", rewardCount = 5,
                    winLine = "Meister Volter: Der Sturm gehoert jetzt dir.")),
            NpcDef("a5_t1", 3, 7, 3, "npc_m", NpcKind.TRAINER, sight = 2,
                lines = listOf("Techniker Ole: Vorsicht, Hochspannung!"),
                afterLines = listOf("Techniker Ole: Kurzschluss ... du bist gut."),
                trainer = TrainerDef("a5_t1", "Techniker Ole",
                    listOf(TrainerMon("zappel", 32), TrainerMon("funkling", 33)), 2800)),
            NpcDef("a5_t2", 9, 7, 2, "npc_f", NpcKind.TRAINER, sight = 2,
                lines = listOf("Blitzableiterin Ute: Ich leite deinen Angriff einfach ab!"),
                afterLines = listOf("Blitzableiterin Ute: Nicht schlecht!"),
                trainer = TrainerDef("a5_t2", "Blitzableiterin Ute",
                    listOf(TrainerMon("voltrax", 34), TrainerMon("sturmfalk", 33)), 3000))
        )
    )

    // ==================================================================
    //  Schattental + Hort
    // ==================================================================
    private val schattental = GameMap(
        id = "schattental", name = "Schattental", theme = "schatten",
        rowsRaw = listOf(
            "#^^^^^^^^^^^^^^^^^^#",
            "#^^^^^^^^^D^^^^^^^^#",
            "#........,,........#",
            "#..gggg..,,..gggg..#",
            "#..gggg..,,..gggg..#",
            "#........,,........#",
            "#..^^^...,,....^^..#",
            "#..^^^...,,....^^..#",
            "#........,,........#",
            ".........,,.........",
            "#........,,........#",
            "#..gggg..,,..gggg..#",
            "#..gggg..,,..gggg..#",
            "#........,,........#",
            "#..^^^^......^^^^..#",
            "#..................#",
            "#..................#",
            "####################"
        ),
        warps = listOf(
            Warp(0, 9, "sturmfeste", 18, 9),
            Warp(10, 1, "hort", 9, 13),
            Warp(19, 9, "ligator", 1, 9, requiresFlag = "morgana_besiegt")
        ),
        npcs = listOf(
            NpcDef("st_sign", 11, 2, 0, "sign", NpcKind.SIGN,
                lines = listOf("HORT DES SCHATTENORDENS - Zutritt verboten!")),
            NpcDef("st_item", 3, 16, 0, "item", NpcKind.ITEM, itemId = "topbeleber", itemCount = 2),
            NpcDef("st_t1", 8, 5, 3, "npc_dark", NpcKind.TRAINER, sight = 3,
                lines = listOf("Schattenorden-Wache: Kehr um, solange du noch kannst!"),
                afterLines = listOf("Schattenorden-Wache: Morgana ist im Hort ..."),
                trainer = TrainerDef("st_t1", "Schattenorden-Wache",
                    listOf(TrainerMon("nachtgeist", 33), TrainerMon("toxarr", 34)), 3400)),
            NpcDef("st_t2", 12, 12, 2, "npc_dark", NpcKind.TRAINER, sight = 3,
                lines = listOf("Schattenorden-Wache: Der Schatten verschlingt dich!"),
                afterLines = listOf("Schattenorden-Wache: Unmoeglich ..."),
                trainer = TrainerDef("st_t2", "Schattenorden-Wache",
                    listOf(TrainerMon("mothara", 33), TrainerMon("schemen", 33), TrainerMon("umbrax", 35)), 3600))
        ),
        encounters = listOf(
            Enc("schemen", 30, 33, 25), Enc("nachtgeist", 32, 35, 10), Enc("toxarr", 31, 34, 20),
            Enc("rattmar", 30, 33, 20), Enc("kampfax", 31, 34, 15), Enc("mothara", 31, 34, 10)
        )
    )

    private val hort = GameMap(
        id = "hort", name = "Hort des Schattenordens", indoor = true, theme = "schatten",
        rowsRaw = listOf(
            "wwwwwwwwwwwwwwwwwwww",
            "woooooooooooooooooow",
            "wobbwwwoooooowwwbbow",
            "wooooowoooooowooooow",
            "wooooowoooooowooooow",
            "woooooooo==oooooooow",
            "wooowwoo====oowwooow",
            "wooowoooo==oooowooow",
            "woooooooooooooooooow",
            "wobbwwwoooooowwwbbow",
            "wooooowoooooowooooow",
            "wooooowoooooowooooow",
            "woooooooooooooooooow",
            "woooooooo==oooooooow",
            "wwwwwwwwwDwwwwwwwwww"
        ),
        warps = listOf(Warp(9, 14, "schattental", 10, 2)),
        npcs = listOf(
            NpcDef("hort_boss", 9, 2, 0, "npc_boss6", NpcKind.TRAINER,
                lines = listOf(
                    "Morgana: Du bist also der Trainer, der meine Leute aufhaelt.",
                    "Morgana: Titanox schlaeft seit tausend Jahren unter dem Gipfel.",
                    "Morgana: Mit seiner Macht forme ich diese Welt neu!",
                    "Morgana: Aber zuerst zerbreche ich dich."
                ),
                afterLines = listOf(
                    "Morgana: ... Wie? Meine Schatten weichen zurueck.",
                    "Morgana: Vielleicht war der Weg der Macht der falsche.",
                    "Morgana: Nimm den GIPFELPASS. Wenn jemand Titanox gegenuebertreten darf, dann du.",
                    "Der Weg nach Osten ins Ligator-Tal ist nun frei!"
                ),
                trainer = TrainerDef("boss6", "Ordensmeisterin Morgana",
                    listOf(
                        TrainerMon("toxarr", 38, listOf("schlammbombe", "toxin", "saeure", "erdbeben")),
                        TrainerMon("mothara", 39, listOf("kaefergebrumm", "schlafpuder", "matschbombe", "silberhauch")),
                        TrainerMon("nachtgeist", 40, listOf("spukball", "konfustrahl", "schattenklaue", "toxin")),
                        TrainerMon("umbrax", 42, listOf("spukball", "schlammbombe", "konfustrahl", "schattenklaue"))
                    ),
                    money = 9000, boss = true, badge = "morgana_besiegt",
                    rewardItem = "gipfelpass",
                    winLine = "Morgana: Der Schatten hat einen Meister gefunden.")),
            NpcDef("hort_t1", 6, 6, 3, "npc_dark", NpcKind.TRAINER, sight = 3,
                lines = listOf("Ordensbruder Kai: Niemand stoert Morganas Ritual!"),
                afterLines = listOf("Ordensbruder Kai: Wie kannst du so stark sein?"),
                trainer = TrainerDef("hort_t1", "Ordensbruder Kai",
                    listOf(TrainerMon("schemen", 35), TrainerMon("giftling", 35), TrainerMon("nachtgeist", 37)), 4000)),
            NpcDef("hort_t2", 13, 6, 2, "npc_dark", NpcKind.TRAINER, sight = 3,
                lines = listOf("Ordensschwester Ria: Der Schatten sieht alles!"),
                afterLines = listOf("Ordensschwester Ria: Geh nur ... sie erwartet dich."),
                trainer = TrainerDef("hort_t2", "Ordensschwester Ria",
                    listOf(TrainerMon("toxarr", 36), TrainerMon("mothara", 37)), 4200)),
            NpcDef("hort_item", 2, 12, 0, "item", NpcKind.ITEM, itemId = "hyperball", itemCount = 5),
            NpcDef("hort_item2", 17, 12, 0, "item", NpcKind.ITEM, itemId = "toptrank", itemCount = 2)
        )
    )

    // ==================================================================
    //  Ligator + Liga + Drachengipfel
    // ==================================================================
    private val ligator = GameMap(
        id = "ligator", name = "Ligator",
        rowsRaw = listOf(
            "####################",
            "#..................#",
            "#....MM......MM....#",
            "#..................#",
            "#....HH......TT....#",
            "#....S.......S.....#",
            "#..................#",
            "#...RRRRRRRRRR.....#",
            "#...RRRRRRRRRR.....#",
            "....BWWWWDWWWWB....#",
            "#..................#",
            "#....,,,,,,,,......#",
            "#..................#",
            "#..RRRR......RRRR..#",
            "#..BWWB......BWWB..#",
            "#..................#",
            "#..................#",
            "####################"
        ),
        warps = listOf(
            Warp(0, 9, "schattental", 18, 9),
            Warp(9, 9, "liga", 7, 12)
        ),
        npcs = listOf(
            NpcDef("lg_nurse", 7, 4, 2, "npc_f", NpcKind.TALK,
                lines = listOf(
                    "Heile dein Team, bevor du die Liga betrittst.",
                    "Danach gibt es kein Zurueck mehr!"
                )),
            NpcDef("lg_sign1", 5, 5, 0, "sign", NpcKind.SIGN,
                lines = listOf("HEILSTATION - Letzte Station vor der Liga.")),
            NpcDef("lg_sign2", 13, 5, 0, "sign", NpcKind.SIGN,
                lines = listOf("GROSSMARKT VON LIGATOR - Alles fuer den letzten Kampf.")),
            NpcDef("lg_wache", 12, 10, 1, "npc_m", NpcKind.TALK,
                lines = listOf(
                    "Ligawache: Nur wer alle fuenf Siegel besitzt, darf hinein.",
                    "Ligawache: Champion Drakon hat noch nie verloren."
                )),
            NpcDef("lg_item", 16, 16, 0, "item", NpcKind.ITEM, itemId = "toptrank", itemCount = 3)
        ),
        shop = shopF
    )

    private val liga = GameMap(
        id = "liga", name = "Liga-Halle", indoor = true, theme = "liga",
        rowsRaw = listOf(
            "wwwwwwwwwwwwwww",
            "wAAAAAADAAAAAAw",
            "wAAAAAAAAAAAAAw",
            "wAAAAAMMMAAAAAw",
            "wAAAAAAAAAAAAAw",
            "wAAAAAAAAAAAAAw",
            "wAAMAAAAAAAMAAw",
            "wAAAAAAAAAAAAAw",
            "wAAAAAAAAAAAAAw",
            "wAAMAAAAAAAMAAw",
            "wAAAAAAAAAAAAAw",
            "wAAAAAAAAAAAAAw",
            "wAAAAA=A=AAAAAw",
            "wwwwwwwDwwwwwww"
        ),
        warps = listOf(
            Warp(7, 13, "ligator", 9, 10),
            Warp(7, 1, "gipfel", 7, 12, requiresFlag = "champion_besiegt")
        ),
        npcs = listOf(
            NpcDef("liga_champ", 7, 4, 0, "npc_champ", NpcKind.TRAINER,
                lines = listOf(
                    "Champion Drakon: Also hast du es tatsaechlich hierher geschafft.",
                    "Champion Drakon: Ich habe von deinem Sieg ueber Morgana gehoert.",
                    "Champion Drakon: Aber die Liga ist eine andere Liga - im Wortsinn.",
                    "Champion Drakon: Zeig mir alles, was du gelernt hast!"
                ),
                afterLines = listOf(
                    "Champion Drakon: Ich bin besiegt. Nach all den Jahren.",
                    "Champion Drakon: Du bist der neue Champion von Auronia!",
                    "Champion Drakon: Und noch etwas: Der Weg zum Drachengipfel steht dir jetzt offen.",
                    "Champion Drakon: Dort oben schlaeft Titanox. Sei vorsichtig."
                ),
                trainer = TrainerDef("champ", "Champion Drakon",
                    listOf(
                        TrainerMon("kampfax", 44, listOf("nahkampf", "steinkante", "erdbeben", "kreuzhieb")),
                        TrainerMon("gigavolt", 45, listOf("donner", "erdbeben", "steinkante", "donnerblitz")),
                        TrainerMon("tidalon", 46, listOf("hydropumpe", "blizzard", "panzerschutz", "surfer")),
                        TrainerMon("draconar", 46, listOf("drachenpuls", "drachentanz", "eisstrahl", "drachenklaue")),
                        TrainerMon("draconis", 49, listOf("drachenpuls", "feuersturm", "drachentanz", "wutanfall"))
                    ),
                    money = 20000, boss = true, badge = "champion_besiegt",
                    rewardItem = "meisterball",
                    winLine = "Champion Drakon: Ein wuerdiger Nachfolger.")),
            NpcDef("liga_t1", 3, 8, 3, "npc_m", NpcKind.TRAINER, sight = 2,
                lines = listOf("Liga-Vorkaempfer Ares: Erst musst du an mir vorbei!"),
                afterLines = listOf("Liga-Vorkaempfer Ares: Der Champion wartet."),
                trainer = TrainerDef("liga_t1", "Liga-Vorkaempfer Ares",
                    listOf(TrainerMon("kampfax", 40), TrainerMon("felsberg", 41), TrainerMon("marino", 42)), 6000)),
            NpcDef("liga_t2", 11, 8, 2, "npc_f", NpcKind.TRAINER, sight = 2,
                lines = listOf("Liga-Vorkaempferin Nyx: Ich pruefe deine Entschlossenheit!"),
                afterLines = listOf("Liga-Vorkaempferin Nyx: Beeindruckend. Geh weiter."),
                trainer = TrainerDef("liga_t2", "Liga-Vorkaempferin Nyx",
                    listOf(TrainerMon("nachtgeist", 41), TrainerMon("floranix", 41), TrainerMon("vulkanix", 43)), 6500))
        )
    )

    private val gipfel = GameMap(
        id = "gipfel", name = "Drachengipfel", theme = "gipfel",
        rowsRaw = listOf(
            "^^^^^^^^^^^^^^^",
            "^^^^^CCCCC^^^^^",
            "^^^CCCCCCCCC^^^",
            "^^CCCCCCCCCCC^^",
            "^^CCCCCCCCCCC^^",
            "^^CCCCMMMCCCC^^",
            "^^CCCCCCCCCCC^^",
            "^^CCCCCCCCCCC^^",
            "^^^CCCCCCCCC^^^",
            "^^^CCCCCCCCC^^^",
            "^^^^CCCCCCC^^^^",
            "^^^^^CCCCC^^^^^",
            "^^^^^CCCCC^^^^^",
            "^^^^^CCDCC^^^^^",
            "^^^^^^^^^^^^^^^"
        ),
        warps = listOf(Warp(7, 13, "liga", 7, 2)),
        npcs = listOf(
            NpcDef("titanox", 7, 4, 0, "npc_legend", NpcKind.TALK,
                lines = listOf(
                    "Ein gewaltiges Bruellen erschuettert den Gipfel!",
                    "TITANOX erwacht aus seinem tausendjaehrigen Schlaf!"
                ),
                afterLines = listOf("Der Gipfel liegt still da. Nur der Wind pfeift.")),
            NpcDef("gipfel_sign", 6, 12, 0, "sign", NpcKind.SIGN,
                lines = listOf("DRACHENGIPFEL - Hier schlaeft der Riese. Wecke ihn nur, wenn du bereit bist."))
        )
    )

    // ==================================================================

    private val maps: Map<String, GameMap> = listOf(
        heim, zuhause, labor, route1, kupferstadt, arena1, route2, hoehle,
        wellenstadt, arena2, route3, aschenfeld, arena3, route4, frostheim,
        arena4, route5, sturmfeste, arena5, schattental, hort, ligator, liga, gipfel
    ).associateBy { it.id }

    fun get(id: String): GameMap = maps[id] ?: heim
    fun all(): Collection<GameMap> = maps.values
}
