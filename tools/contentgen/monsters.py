"""Generates ``monsters.json`` — every species of the Bestiarium.

Content is authored as compact *family* seeds (one row per evolution line) and
expanded into full species entries: stats derived from the family's combat
role and evolution stage, learnsets drawn from the generated move pool, lore
composed from the family's authored sentence plus a stage-specific clause.

All names are original coinages built from Old-Norse-flavoured German roots.
No name, design or text is taken from an existing franchise; the mythological
figures they allude to (Fenrir, Jörmungandr, the Norns …) are public domain.
"""

from __future__ import annotations

from common import (
    ELEMENT_DE, ELEMENT_EN, Strings, base_experience, catch_rate, rng, slug, stat_block,
)

# ---------------------------------------------------------------------------
# Family seeds
# ---------------------------------------------------------------------------
# (family_id, [stage names], primary, secondary, world, rarity, role,
#  egg_groups, size, growth, lore_de, lore_en)

FAMILIES = [
    # --- Midgard ---------------------------------------------------------
    ("glutwelp", ["Glutwelp", "Glutfang", "Glutjarl"], "FIRE", None, "MIDGARD", "COMMON",
     "attacker", ["beast"], "SMALL", "STEADY",
     "Sie schlafen in erloschenen Herdfeuern und wachen auf, wenn jemand friert.",
     "They sleep in cold hearths and wake whenever someone is freezing."),
    ("moosling", ["Moosling", "Hainhorn", "Haingeweih"], "NATURE", None, "MIDGARD", "COMMON",
     "tank", ["beast", "flora"], "MEDIUM", "SLOW",
     "Auf ihrem Rücken wächst ein ganzer kleiner Wald mit.",
     "An entire small forest grows along on their backs."),
    ("bachotter", ["Bachotter", "Stromotter", "Fjordherr"], "WATER", None, "MIDGARD", "COMMON",
     "speedster", ["beast", "aquatic"], "SMALL", "SWIFT",
     "Sie folgen Flüssen bis zum Meer und wieder zurück, ihr Leben lang.",
     "They follow rivers to the sea and back again, all their lives."),
    ("steinkauz", ["Steinkauz", "Felsaug", "Bergwächter"], "EARTH", "WIND", "MIDGARD", "UNCOMMON",
     "wall", ["avian"], "SMALL", "STEADY",
     "Ihr Ruf verrät Wanderern, wo der Pfad sicher ist.",
     "Their call tells travellers where the path is safe."),
    ("wanderkraehe", ["Wanderkrähe", "Rabenbote", "Hugimund"], "WIND", "SHADOW", "MIDGARD", "UNCOMMON",
     "mage", ["avian"], "SMALL", "STEADY",
     "Sie tragen Nachrichten zwischen Dörfern — und angeblich zwischen Welten.",
     "They carry messages between villages — and, they say, between worlds."),
    ("feldhopser", ["Feldhopser", "Wiesensprung", "Erntekönig"], "NATURE", None, "MIDGARD", "COMMON",
     "speedster", ["beast"], "TINY", "SWIFT",
     "Wo sie hüpfen, wächst das Korn im nächsten Jahr doppelt so hoch.",
     "Where they hop, next year's grain grows twice as tall."),
    ("weidling", ["Weidling", "Ulmenhüter", "Eschenwart"], "NATURE", "EARTH", "MIDGARD", "UNCOMMON",
     "tank", ["flora"], "LARGE", "SLOW",
     "Ein Eschenwart erinnert sich an jeden, der je in seinem Schatten rastete.",
     "An ash warden remembers everyone who ever rested in its shade."),
    ("wellenrobbe", ["Wellenrobbe", "Brandungsbulle"], "WATER", None, "MIDGARD", "COMMON",
     "bruiser", ["aquatic"], "MEDIUM", "STEADY",
     "Ihr Gebrüll kündigt Stürme an, lange bevor der Himmel sich verdunkelt.",
     "Their bellow announces storms long before the sky darkens."),
    ("hofwelpe", ["Hofwelpe", "Hofwächter", "Torhund"], "EARTH", "METAL", "MIDGARD", "COMMON",
     "bruiser", ["beast"], "MEDIUM", "STEADY",
     "Sie wählen sich ein Tor und verteidigen es, auch wenn niemand mehr dahinter wohnt.",
     "They pick a gate and defend it, even when no one lives behind it any more."),

    # --- Asgard ----------------------------------------------------------
    ("lichtfunke", ["Lichtfunke", "Glanzbote", "Asenbote"], "LIGHT", None, "ASGARD", "UNCOMMON",
     "mage", ["spirit"], "TINY", "STEADY",
     "Funken, die von den Fackeln der Götterhalle absprangen und nie erloschen.",
     "Sparks that leapt from the torches of the gods' hall and never went out."),
    ("speerschemen", ["Speerschemen", "Schildgeist", "Walkürengeist"], "DIVINE", "METAL", "ASGARD", "RARE",
     "attacker", ["spirit"], "MEDIUM", "SLOW",
     "Die Erinnerung an eine Schildmaid, die ihren Wall nie brechen ließ.",
     "The memory of a shield-maiden who never let her wall break."),
    ("hallenkrieger", ["Hallenkrieger", "Einherjer", "Ewigkrieger"], "SPIRIT", "METAL", "ASGARD", "RARE",
     "bruiser", ["spirit"], "MEDIUM", "SLOW",
     "Jeden Morgen fallen sie, jeden Abend trinken sie zusammen.",
     "Every morning they fall; every evening they drink together."),
    ("goldkuecken", ["Goldkücken", "Goldhahn", "Gullinkamm"], "LIGHT", "DIVINE", "ASGARD", "RARE",
     "support", ["avian"], "SMALL", "SLOW",
     "Sein Krähen weckt die Gefallenen — und warnt vor dem letzten Morgen.",
     "Its crow wakes the fallen — and warns of the final morning."),
    ("brueckenwicht", ["Brückenwicht", "Brückenwart", "Bifröstwart"], "LIGHT", "RUNE", "ASGARD", "RARE",
     "wall", ["spirit", "construct"], "MEDIUM", "GLACIAL",
     "Sie zählen jeden, der die Regenbogenbrücke betritt. Sie vergessen niemanden.",
     "They count everyone who steps on the rainbow bridge. They forget no one."),
    ("himmelszicke", ["Himmelszicke", "Himmelsbock", "Donnerbock"], "THUNDER", "NATURE", "ASGARD", "UNCOMMON",
     "bruiser", ["beast"], "MEDIUM", "STEADY",
     "Man sagt, zwei von ihnen zögen einst einen Wagen über die Wolken.",
     "Two of them, it is said, once drew a chariot across the clouds."),
    ("gedankenrabe", ["Gedankenrabe", "Erinnerungsrabe"], "SHADOW", "DIVINE", "ASGARD", "EPIC",
     "mage", ["avian"], "SMALL", "SLOW",
     "Einer fliegt aus und sieht alles. Der andere kehrt zurück und vergisst nichts.",
     "One flies out and sees everything. The other returns and forgets nothing."),
    ("speersplitter", ["Speersplitter", "Speergeist", "Gungnirwacht"], "METAL", "DIVINE", "ASGARD", "EPIC",
     "attacker", ["construct"], "SMALL", "GLACIAL",
     "Splitter eines Speers, der sein Ziel nie verfehlte — und es nie vergaß.",
     "Splinters of a spear that never missed its mark — and never forgot it."),
    ("metfunke", ["Metfunke", "Metgeist", "Methallengeist"], "SPIRIT", "LIGHT", "ASGARD", "UNCOMMON",
     "support", ["spirit"], "TINY", "STEADY",
     "Sie entstehen aus dem letzten Schluck in einem Horn, den niemand mehr trank.",
     "They form from the last mouthful in a horn that no one drank."),

    # --- Vanaheim --------------------------------------------------------
    ("saatgeist", ["Saatgeist", "Ährengeist", "Erntemutter"], "NATURE", None, "VANAHEIM", "COMMON",
     "support", ["flora", "spirit"], "SMALL", "STEADY",
     "Ohne sie schläft das Korn im Boden und wacht nie auf.",
     "Without them the grain sleeps in the soil and never wakes."),
    ("goldfuchs", ["Goldfuchs", "Sonnenfuchs", "Freyfuchs"], "NATURE", "LIGHT", "VANAHEIM", "UNCOMMON",
     "speedster", ["beast"], "SMALL", "SWIFT",
     "Ihr Fell hält das Licht des Spätsommers fest, bis in den tiefsten Winter.",
     "Their fur holds late-summer light deep into winter."),
    ("borstenferkel", ["Borstenferkel", "Goldborste", "Goldeber"], "EARTH", "METAL", "VANAHEIM", "UNCOMMON",
     "bruiser", ["beast"], "MEDIUM", "SLOW",
     "Seine Borsten leuchten im Dunkeln hell genug, um einen Weg zu finden.",
     "Its bristles glow brightly enough to light a path in the dark."),
    ("seidrkaetzchen", ["Seidrkätzchen", "Seidrkatze", "Seidrpanther"], "SPIRIT", "RUNE", "VANAHEIM", "RARE",
     "mage", ["beast", "spirit"], "SMALL", "STEADY",
     "Sie schnurren in Runen. Wer lange genug zuhört, versteht sie.",
     "They purr in runes. Listen long enough and you understand them."),
    ("bluetenwicht", ["Blütenwicht", "Blütenherz", "Blütenkönigin"], "NATURE", "WATER", "VANAHEIM", "UNCOMMON",
     "support", ["flora"], "SMALL", "STEADY",
     "Eine Blütenkönigin lässt ein ganzes Tal an einem Morgen erblühen.",
     "A bloom queen makes an entire valley flower in a single morning."),
    ("quellnymphe", ["Quellnymphe", "Stromnymphe", "Flussmutter"], "WATER", None, "VANAHEIM", "UNCOMMON",
     "mage", ["aquatic", "spirit"], "MEDIUM", "STEADY",
     "Jeder Fluss der Neun Welten hat eine Mutter. Manche sind gnädig.",
     "Every river of the Nine has a mother. Some of them are kind."),
    ("honigsumme", ["Honigsumme", "Metbraue", "Metkönigin"], "NATURE", "WIND", "VANAHEIM", "COMMON",
     "speedster", ["insect"], "TINY", "SWIFT",
     "Ihr Honig wird zu einem Met, der Erinnerungen zurückbringt.",
     "Their honey becomes a mead that brings memories back."),
    ("sonnenkitz", ["Sonnenkitz", "Sonnenhirsch"], "LIGHT", "NATURE", "VANAHEIM", "RARE",
     "support", ["beast"], "MEDIUM", "SLOW",
     "Wo es äst, endet die Dürre.",
     "Where it grazes, the drought ends."),
    ("seidrflamme", ["Seidrflamme", "Wahrflamme", "Nornenflamme"], "RUNE", "SPIRIT", "VANAHEIM", "EPIC",
     "mage", ["spirit"], "SMALL", "GLACIAL",
     "In ihrem Licht sieht man nicht, was ist — sondern was werden könnte.",
     "In their light you see not what is, but what could be."),

    # --- Alfheim ---------------------------------------------------------
    ("lichtwicht", ["Lichtwicht", "Lichtelf", "Lichtfürst"], "LIGHT", None, "ALFHEIM", "COMMON",
     "mage", ["spirit"], "SMALL", "STEADY",
     "Sie sind kaum mehr als ein Gedanke aus Licht — und doch stolz wie Fürsten.",
     "They are barely more than a thought made of light — and proud as princes."),
    ("glanzfalter", ["Glanzfalter", "Prismafalter"], "LIGHT", "WIND", "ALFHEIM", "COMMON",
     "speedster", ["insect"], "TINY", "SWIFT",
     "Ihre Flügel brechen das Licht in Farben, für die es keine Namen gibt.",
     "Their wings split light into colours that have no names."),
    ("harfenklang", ["Harfenklang", "Sphärenklang"], "SPIRIT", "LIGHT", "ALFHEIM", "UNCOMMON",
     "support", ["spirit"], "SMALL", "STEADY",
     "Man hört sie eher, als dass man sie sieht.",
     "You hear them long before you see them."),
    ("silberspross", ["Silberspross", "Silberbirke", "Silberwart"], "NATURE", "LIGHT", "ALFHEIM", "UNCOMMON",
     "wall", ["flora"], "LARGE", "SLOW",
     "Ihre Rinde spiegelt den Mond, auch bei Tag.",
     "Their bark mirrors the moon, even by day."),
    ("tauperle", ["Tauperle", "Tautänzer"], "WATER", "LIGHT", "ALFHEIM", "COMMON",
     "support", ["spirit", "aquatic"], "TINY", "SWIFT",
     "Sie entstehen im ersten Tau und vergehen mit dem letzten.",
     "They form in the first dew and fade with the last."),
    ("prismasplitter", ["Prismasplitter", "Prismawächter", "Prismafürst"], "RUNE", "LIGHT", "ALFHEIM", "RARE",
     "wall", ["construct"], "MEDIUM", "GLACIAL",
     "Jeder Splitter trägt eine halbe Rune. Zwei zusammen sprechen ein Wort.",
     "Each shard carries half a rune. Two together speak a word."),
    ("daemmerlerche", ["Dämmerlerche", "Morgenlerche"], "WIND", "LIGHT", "ALFHEIM", "COMMON",
     "speedster", ["avian"], "TINY", "SWIFT",
     "Sie singt genau eine Minute vor Sonnenaufgang. Immer.",
     "It sings exactly one minute before sunrise. Always."),
    ("daemmerkitz", ["Dämmerkitz", "Dämmerreh"], "NATURE", "SPIRIT", "ALFHEIM", "UNCOMMON",
     "speedster", ["beast"], "MEDIUM", "STEADY",
     "Zwischen Tag und Nacht steht es still und ist für einen Atemzug unsichtbar.",
     "Between day and night it stands still and is invisible for one breath."),
    ("sternflaum", ["Sternflaum", "Sternenschweif", "Sternenherr"], "LIGHT", "DIVINE", "ALFHEIM", "EPIC",
     "mage", ["spirit"], "SMALL", "GLACIAL",
     "Es fällt vom Himmel, wenn irgendwo jemand etwas Unmögliches wünscht.",
     "It falls from the sky whenever, somewhere, someone wishes for the impossible."),

    # --- Jötunheim -------------------------------------------------------
    ("frostwicht", ["Frostwicht", "Frostriese", "Frostjarl"], "ICE", "EARTH", "JOTUNHEIM", "COMMON",
     "tank", ["giant"], "LARGE", "SLOW",
     "Selbst der kleinste von ihnen wirft einen Schatten wie ein Haus.",
     "Even the smallest of them casts a shadow like a house."),
    ("geroelltroll", ["Gerölltroll", "Felstroll", "Bergtroll"], "EARTH", None, "JOTUNHEIM", "COMMON",
     "tank", ["giant"], "LARGE", "SLOW",
     "Bei Tageslicht sind sie kaum von einem Felsblock zu unterscheiden. Das ist Absicht.",
     "In daylight they are hard to tell from a boulder. That is deliberate."),
    ("sturmadler", ["Sturmadler", "Wetteradler", "Sturmschwinge"], "WIND", None, "JOTUNHEIM", "UNCOMMON",
     "speedster", ["avian"], "LARGE", "STEADY",
     "Jeder ihrer Flügelschläge ist irgendwo unten ein Sturm.",
     "Every beat of their wings is a storm somewhere below."),
    ("winterwelpe", ["Winterwelpe", "Winterwolf", "Winterjäger"], "ICE", None, "JOTUNHEIM", "UNCOMMON",
     "attacker", ["beast"], "MEDIUM", "STEADY",
     "Sie jagen im Rudel und hinterlassen nur eine Spur — die des Ersten.",
     "They hunt as a pack and leave a single track — the first one's."),
    ("urrind", ["Urrind", "Urstier", "Urhorn"], "EARTH", "NATURE", "JOTUNHEIM", "UNCOMMON",
     "tank", ["beast"], "HUGE", "GLACIAL",
     "Aus dem Reif am Anfang aller Dinge leckte einst ein solches Tier die Welt frei.",
     "From the rime at the beginning of things, such a beast once licked the world free."),
    ("eiszapfling", ["Eiszapfling", "Eiszahn", "Eiskoloss"], "ICE", None, "JOTUNHEIM", "COMMON",
     "bruiser", ["giant"], "MEDIUM", "SLOW",
     "Was sie berühren, bleibt kalt — Tage, manchmal Jahre.",
     "What they touch stays cold — for days, sometimes years."),
    ("steinfaust", ["Steinfaust", "Felsfaust", "Bergfaust"], "EARTH", "METAL", "JOTUNHEIM", "UNCOMMON",
     "bruiser", ["giant"], "LARGE", "SLOW",
     "Ein Schlag, ein Tal.",
     "One blow, one valley."),
    ("erdwurm", ["Erdwurm", "Grabwurm", "Schlundwurm"], "EARTH", "CHAOS", "JOTUNHEIM", "RARE",
     "attacker", ["wyrm"], "HUGE", "SLOW",
     "Die Gänge, die sie graben, führen manchmal in eine andere Welt.",
     "The tunnels they dig sometimes come out in another world."),
    ("boeenwicht", ["Böenwicht", "Böengeist", "Orkangeist"], "WIND", "ICE", "JOTUNHEIM", "UNCOMMON",
     "mage", ["spirit"], "SMALL", "SWIFT",
     "Sie sind der Grund, warum man in Jötunheim nie den Mund öffnet, wenn es pfeift.",
     "They are why no one in Jotunheim opens their mouth when the wind whistles."),

    # --- Muspelheim ------------------------------------------------------
    ("funkenwicht", ["Funkenwicht", "Flammengeist", "Muspelgeist"], "FIRE", None, "MUSPELHEIM", "COMMON",
     "mage", ["spirit"], "TINY", "STEADY",
     "Sie kennen keine Kälte und begreifen nicht, dass andere sie fürchten.",
     "They know no cold, and cannot grasp that others fear it."),
    ("glutmolch", ["Glutmolch", "Lavamolch", "Magmadrache"], "FIRE", "EARTH", "MUSPELHEIM", "UNCOMMON",
     "bruiser", ["wyrm"], "MEDIUM", "SLOW",
     "Sie schwimmen in Lava, wie andere in einem Bach.",
     "They swim in lava the way others swim in a brook."),
    ("feuerklinge", ["Feuerklinge", "Flammenklinge", "Surtklinge"], "FIRE", "METAL", "MUSPELHEIM", "RARE",
     "attacker", ["construct"], "MEDIUM", "SLOW",
     "Eine Klinge, die schon brannte, bevor jemand sie schmiedete.",
     "A blade that was already burning before anyone forged it."),
    ("aschenvogel", ["Aschenvogel", "Ascheschwinge", "Glutphönix"], "FIRE", "LIGHT", "MUSPELHEIM", "RARE",
     "mage", ["avian"], "MEDIUM", "GLACIAL",
     "Es stirbt nicht. Es wiederholt sich nur.",
     "It does not die. It merely repeats itself."),
    ("glutspuerer", ["Glutspürer", "Höllenspürer"], "FIRE", "SHADOW", "MUSPELHEIM", "UNCOMMON",
     "attacker", ["beast"], "MEDIUM", "STEADY",
     "Sie riechen Angst durch Stein hindurch.",
     "They smell fear straight through stone."),
    ("schlackenstein", ["Schlackenstein", "Basaltwacht", "Basaltkoloss"], "EARTH", "FIRE", "MUSPELHEIM", "UNCOMMON",
     "tank", ["construct"], "LARGE", "GLACIAL",
     "Was in Muspelheim erkaltet, wird zu ihnen.",
     "Whatever cools in Muspelheim becomes one of them."),
    ("rauchschemen", ["Rauchschemen", "Aschgeist"], "SPIRIT", "FIRE", "MUSPELHEIM", "COMMON",
     "glass", ["spirit"], "SMALL", "SWIFT",
     "Man kann sie nicht greifen, aber sie greifen einen.",
     "You cannot grasp them, but they can grasp you."),
    ("glutriss", ["Glutriss", "Feuerriss", "Weltenriss"], "CHAOS", "FIRE", "MUSPELHEIM", "EPIC",
     "glass", ["chaos"], "MEDIUM", "GLACIAL",
     "Ein Riss ist kein Wesen. Dieser hier hat es trotzdem gelernt.",
     "A rift is not a creature. This one learned to be anyway."),
    ("lavaschreiter", ["Lavaschreiter", "Magmaschreiter"], "FIRE", "METAL", "MUSPELHEIM", "UNCOMMON",
     "bruiser", ["insect"], "LARGE", "SLOW",
     "Ihr Panzer wird härter, je heißer es wird.",
     "Their carapace hardens the hotter it gets."),

    # --- Niflheim --------------------------------------------------------
    ("nebelwicht", ["Nebelwicht", "Nebelgeist", "Nebelherr"], "SPIRIT", "ICE", "NIFLHEIM", "COMMON",
     "mage", ["spirit"], "SMALL", "STEADY",
     "Im Nebel von Niflheim ist alles ein Nebelwicht, bis es sich bewegt.",
     "In Niflheim's mist everything is a mist wight, until it moves."),
    ("reifling", ["Reifling", "Reifwolf", "Reifjäger"], "ICE", None, "NIFLHEIM", "COMMON",
     "attacker", ["beast"], "MEDIUM", "STEADY",
     "Ihre Spur bleibt sichtbar, lange nachdem sie fort sind.",
     "Their tracks stay visible long after they are gone."),
    ("nidschuppe", ["Nidschuppe", "Nidwyrm", "Nagwyrm"], "SHADOW", "CHAOS", "NIFLHEIM", "RARE",
     "attacker", ["wyrm"], "HUGE", "GLACIAL",
     "Etwas nagt an der Wurzel der Welt. Diese hier haben davon gelernt.",
     "Something gnaws at the root of the world. These learned from it."),
    ("quellhauch", ["Quellhauch", "Brunnengeist"], "WATER", "ICE", "NIFLHEIM", "UNCOMMON",
     "support", ["aquatic", "spirit"], "SMALL", "STEADY",
     "Aus einem einzigen Brunnen entspringen elf Flüsse. Sie bewachen ihn.",
     "Eleven rivers rise from a single well. They guard it."),
    ("frostnatter", ["Frostnatter", "Frostschlange"], "ICE", "SHADOW", "NIFLHEIM", "UNCOMMON",
     "glass", ["wyrm"], "MEDIUM", "SWIFT",
     "Ihr Biss friert nicht die Haut, sondern den Mut.",
     "Their bite freezes not the skin, but the courage."),
    ("nebelkauz", ["Nebelkauz", "Nebeleule"], "SPIRIT", "WIND", "NIFLHEIM", "COMMON",
     "speedster", ["avian"], "SMALL", "SWIFT",
     "Man hört ihre Flügel nie. Nur ihren Ruf, und der kommt immer von hinten.",
     "You never hear their wings. Only their call, and it always comes from behind."),
    ("firnsplitter", ["Firnsplitter", "Firnkristall", "Firnkoloss"], "ICE", "METAL", "NIFLHEIM", "UNCOMMON",
     "wall", ["construct"], "MEDIUM", "GLACIAL",
     "Sie wachsen aus Eis, das nie geschmolzen ist, seit die Welt begann.",
     "They grow from ice that has never melted since the world began."),
    ("totholz", ["Totholz", "Frostholz"], "NATURE", "ICE", "NIFLHEIM", "COMMON",
     "tank", ["flora"], "MEDIUM", "SLOW",
     "Tot, aber nicht ruhig.",
     "Dead, but not resting."),
    ("kaeltehauch", ["Kältehauch", "Kältesturm"], "ICE", "WIND", "NIFLHEIM", "UNCOMMON",
     "mage", ["spirit"], "SMALL", "SWIFT",
     "Der erste Frost eines Jahres ist immer einer von ihnen.",
     "The first frost of the year is always one of them."),

    # --- Helheim ---------------------------------------------------------
    ("draugwicht", ["Draugwicht", "Draugkrieger", "Draugjarl"], "SPIRIT", "SHADOW", "HELHEIM", "COMMON",
     "bruiser", ["undead"], "MEDIUM", "SLOW",
     "Sie bewachen Grabhügel, in denen längst nichts mehr liegt.",
     "They guard barrows that have been empty for centuries."),
    ("grabwelpe", ["Grabwelpe", "Grabhund", "Garmwacht"], "SHADOW", None, "HELHEIM", "UNCOMMON",
     "attacker", ["beast", "undead"], "MEDIUM", "STEADY",
     "Am Tor der Totenhalle sitzt immer einer. Immer.",
     "One always sits at the gate of the hall of the dead. Always."),
    ("knochenwicht", ["Knochenwicht", "Knochenwacht", "Knochenfürst"], "SPIRIT", "METAL", "HELHEIM", "COMMON",
     "tank", ["undead"], "MEDIUM", "SLOW",
     "Sie bauen sich aus dem, was sie finden. Manchmal aus mehreren.",
     "They build themselves from whatever they find. Sometimes from several."),
    ("leichenrabe", ["Leichenrabe", "Todesrabe"], "SHADOW", "SPIRIT", "HELHEIM", "COMMON",
     "speedster", ["avian"], "SMALL", "SWIFT",
     "Wo drei von ihnen kreisen, ist jemand noch nicht ganz tot.",
     "Where three of them circle, someone is not quite dead yet."),
    ("totennebel", ["Totennebel", "Seelennebel"], "SPIRIT", None, "HELHEIM", "UNCOMMON",
     "mage", ["spirit"], "MEDIUM", "STEADY",
     "Er besteht aus letzten Atemzügen.",
     "It is made of final breaths."),
    ("irrlicht", ["Irrlicht", "Grablicht", "Seelenlicht"], "SPIRIT", "LIGHT", "HELHEIM", "UNCOMMON",
     "support", ["spirit"], "TINY", "STEADY",
     "Es führt dich nach Hause. Es sagt nur nicht, zu welchem.",
     "It leads you home. It just does not say whose."),
    ("modernatter", ["Modernatter", "Modernschlange"], "CHAOS", "SPIRIT", "HELHEIM", "UNCOMMON",
     "glass", ["wyrm"], "MEDIUM", "SWIFT",
     "Ihr Gift wirkt nicht auf den Körper.",
     "Their venom does not act on the body."),
    ("halbschemen", ["Halbschemen", "Zwiegestalt", "Hallenwart"], "SHADOW", "DIVINE", "HELHEIM", "EPIC",
     "wall", ["undead", "spirit"], "MEDIUM", "GLACIAL",
     "Eine Hälfte lebendig, eine Hälfte nicht — und beide reden mit dir.",
     "One half living, one half not — and both of them speak to you."),
    ("fluchmal", ["Fluchmal", "Fluchgeist", "Fluchherr"], "CHAOS", "SHADOW", "HELHEIM", "RARE",
     "mage", ["chaos", "spirit"], "SMALL", "SLOW",
     "Ein Fluch braucht jemanden, der ihn ausspricht. Diese sprechen sich selbst.",
     "A curse needs someone to speak it. These speak themselves."),

    # --- Svartalfheim ----------------------------------------------------
    ("erzwicht", ["Erzwicht", "Erzknecht", "Erzmeister"], "METAL", None, "SVARTALFHEIM", "COMMON",
     "tank", ["construct"], "SMALL", "SLOW",
     "Sie sortieren Erz nach Güte, ohne dass jemand es ihnen beibrachte.",
     "They sort ore by quality, though nobody taught them how."),
    ("ambossgeist", ["Ambossgeist", "Essengeist", "Schmiedeherr"], "METAL", "FIRE", "SVARTALFHEIM", "RARE",
     "bruiser", ["spirit", "construct"], "MEDIUM", "SLOW",
     "Jeder Amboss, auf dem tausend Nächte lang gehämmert wurde, bekommt eine Seele.",
     "Every anvil hammered on for a thousand nights gains a soul."),
    ("grabpfote", ["Grabpfote", "Tiefpfote", "Tiefengräber"], "EARTH", None, "SVARTALFHEIM", "COMMON",
     "bruiser", ["beast"], "SMALL", "STEADY",
     "Sie graben nicht nach Erz. Sie graben nach Stille.",
     "They do not dig for ore. They dig for silence."),
    ("goldkaefer", ["Goldkäfer", "Goldpanzer"], "METAL", "EARTH", "SVARTALFHEIM", "COMMON",
     "wall", ["insect"], "SMALL", "SLOW",
     "Ihr Panzer ist echtes Gold. Deshalb sind sie selten geworden.",
     "Their shell is true gold. That is why they have grown rare."),
    ("ritzwicht", ["Ritzwicht", "Runenritzer", "Runenmeister"], "RUNE", "METAL", "SVARTALFHEIM", "RARE",
     "mage", ["construct", "spirit"], "SMALL", "GLACIAL",
     "Sie ritzen ohne Unterlass. Niemand weiß, was sie schreiben.",
     "They carve without pause. No one knows what they are writing."),
    ("adersplitter", ["Adersplitter", "Aderkristall"], "EARTH", "RUNE", "SVARTALFHEIM", "UNCOMMON",
     "wall", ["construct"], "MEDIUM", "SLOW",
     "In den Adern des Berges fließt kein Blut, aber etwas fließt.",
     "No blood flows in the mountain's veins, but something does."),
    ("schachtflatterer", ["Schachtflatterer", "Tiefenflatterer"], "SHADOW", "WIND", "SVARTALFHEIM", "COMMON",
     "speedster", ["beast"], "TINY", "SWIFT",
     "Sie hören den Berg atmen und fliehen, bevor er einstürzt.",
     "They hear the mountain breathe and flee before it collapses."),
    ("nagelpuppe", ["Nagelpuppe", "Eisenpuppe", "Eisengolem"], "METAL", None, "SVARTALFHEIM", "UNCOMMON",
     "tank", ["construct"], "MEDIUM", "GLACIAL",
     "Der erste Zwerg, der eine baute, wollte nur Gesellschaft.",
     "The first dwarf who built one only wanted company."),
    ("essenfunke", ["Essenfunke", "Essenglut"], "FIRE", "METAL", "SVARTALFHEIM", "COMMON",
     "glass", ["spirit"], "TINY", "SWIFT",
     "Sie springen aus der Esse und suchen sich etwas zum Anzünden.",
     "They leap from the hearth and look for something to set alight."),

    # --- second wave -----------------------------------------------------
    ("dorfkatze", ["Dorfkatze", "Nachtpirsch", "Schattenpirsch"], "SHADOW", None, "MIDGARD", "COMMON",
     "speedster", ["beast"], "SMALL", "SWIFT",
     "Tagsüber gehören sie jedem im Dorf. Nachts gehören sie niemandem.",
     "By day they belong to everyone in the village. By night, to no one."),
    ("mondblume", ["Mondblume", "Mondblüte", "Mondkrone"], "LIGHT", "NATURE", "ALFHEIM", "UNCOMMON",
     "support", ["flora"], "SMALL", "SLOW",
     "Sie öffnet sich nur, wenn niemand hinsieht — und schließt sich, sobald jemand kommt.",
     "It opens only when nobody is watching, and closes the moment someone arrives."),
    ("schneehopser", ["Schneehopser", "Schneesprung", "Firnhopser"], "ICE", "NATURE", "NIFLHEIM", "COMMON",
     "speedster", ["beast"], "TINY", "SWIFT",
     "Sie graben Tunnel im Schnee, die im Frühjahr ganze Landkarten ergeben.",
     "They dig tunnels in the snow that in spring turn out to be whole maps."),
    ("tiefenflosse", ["Tiefenflosse", "Grubenfisch", "Schluchtwächter"], "WATER", "METAL", "SVARTALFHEIM",
     "UNCOMMON", "wall", ["aquatic"], "MEDIUM", "SLOW",
     "In den gefluteten Stollen leuchten sie den Zwergen den Weg — gegen Bezahlung.",
     "In the flooded shafts they light the dwarves' way — for a fee."),
    ("glutspinne", ["Glutspinne", "Brandspinne", "Aschenweberin"], "FIRE", "CHAOS", "MUSPELHEIM", "RARE",
     "glass", ["insect"], "MEDIUM", "STEADY",
     "Ihre Netze bestehen aus etwas, das noch keine Sprache benannt hat.",
     "Their webs are made of something no language has named yet."),
]

# ---------------------------------------------------------------------------
# Legendary and mythic singles
# ---------------------------------------------------------------------------
# (id, name_de, name_en, primary, secondary, world, rarity, role, size,
#  signature_move, lore_de, lore_en)

LEGENDS = [
    ("surtwacht", "Surtwacht", "Surtwatch", "FIRE", "CHAOS", "MUSPELHEIM", "LEGENDARY",
     "attacker", "COLOSSAL", "mv_weltenbrand",
     "Es wartet am Rand von Muspelheim, das Schwert erhoben, seit die Welt jung war.",
     "It waits at the edge of Muspelheim, sword raised, since the world was young."),
    ("aegirin", "Ägirin", "Aegirin", "WATER", "ICE", "MIDGARD", "LEGENDARY",
     "wall", "COLOSSAL", "mv_urflut",
     "Der Meeresgrund ist ihre Halle, und Ertrunkene sind ihre Gäste.",
     "The sea floor is her hall, and the drowned are her guests."),
    ("hrimthar", "Hrimthar", "Hrimthar", "ICE", "EARTH", "NIFLHEIM", "LEGENDARY",
     "tank", "COLOSSAL", "mv_ewiger_winter",
     "Wo es geht, folgt der Winter, der drei Sommer verschlingt.",
     "Where it walks, the winter that swallows three summers follows."),
    ("vedrspir", "Vedrspir", "Vedrspir", "WIND", "SPIRIT", "JOTUNHEIM", "LEGENDARY",
     "speedster", "HUGE", "mv_sturmgesang",
     "Jeder Wind der Neun Welten beginnt unter seinen Schwingen.",
     "Every wind of the Nine Worlds begins beneath its wings."),
    ("bergrimm", "Bergrimm", "Bergrimm", "EARTH", "METAL", "JOTUNHEIM", "LEGENDARY",
     "tank", "COLOSSAL", "mv_weltengrund",
     "Man hielt es tausend Jahre für ein Gebirge. Dann öffnete es die Augen.",
     "For a thousand years it was taken for a mountain range. Then it opened its eyes."),
    ("ardvin", "Ardvin", "Ardvin", "NATURE", "LIGHT", "VANAHEIM", "LEGENDARY",
     "support", "HUGE", "mv_lebenssaat",
     "Sie trägt den Samen, aus dem nach dem Ende alles wieder wächst.",
     "She carries the seed from which everything regrows after the end."),
    ("thrudvang", "Thrudvang", "Thrudvang", "THUNDER", "DIVINE", "ASGARD", "LEGENDARY",
     "bruiser", "HUGE", "mv_himmelszorn",
     "Sein Schritt ist Donner, sein Atem ist das Wetter.",
     "Its stride is thunder, its breath is the weather."),
    ("dagrun", "Dagrun", "Dagrun", "LIGHT", "DIVINE", "ALFHEIM", "LEGENDARY",
     "mage", "LARGE", "mv_erster_morgen",
     "Sie bringt jeden Morgen zurück — auch den, den niemand verdient hat.",
     "She brings back every morning — even the ones nobody deserved."),
    ("nottvar", "Nottvar", "Nottvar", "SHADOW", "SPIRIT", "HELHEIM", "LEGENDARY",
     "mage", "HUGE", "mv_langnacht",
     "Es ist nicht böse. Es ist nur sehr, sehr geduldig.",
     "It is not evil. It is only very, very patient."),
    ("valdraug", "Valdraug", "Valdraug", "SPIRIT", "SHADOW", "HELHEIM", "LEGENDARY",
     "glass", "LARGE", "mv_seelenernte",
     "Es zählt die Toten und ärgert sich, wenn eine Zahl nicht stimmt.",
     "It counts the dead and grows annoyed when a number is wrong."),
    ("galdrmar", "Galdrmar", "Galdrmar", "RUNE", "DIVINE", "SVARTALFHEIM", "LEGENDARY",
     "mage", "LARGE", "mv_erstes_zeichen",
     "Es kennt die Rune, die vor allen anderen kam.",
     "It knows the rune that came before all the others."),
    ("dvalgrim", "Dvalgrim", "Dvalgrim", "METAL", "EARTH", "SVARTALFHEIM", "LEGENDARY",
     "bruiser", "HUGE", "mv_zwergengericht",
     "Der erste Schmied. Er hört noch immer nicht auf.",
     "The first smith. He still has not stopped."),
    ("fenvarg", "Fenvarg", "Fenvarg", "SHADOW", "CHAOS", "HELHEIM", "MYTHIC",
     "attacker", "COLOSSAL", "mv_ragnaroek",
     "Die Fessel hielt. Sie hält noch. Sie hält gerade so.",
     "The binding held. It still holds. It only just holds."),
    ("jorwyrm", "Jörwyrm", "Jorwyrm", "WATER", "CHAOS", "MIDGARD", "MYTHIC",
     "tank", "COLOSSAL", "mv_urflut",
     "Es liegt um die Welt und beißt sich selbst in den Schwanz. Wenn es loslässt, endet alles.",
     "It lies around the world biting its own tail. When it lets go, everything ends."),
    ("sleiprun", "Sleiprun", "Sleiprun", "WIND", "SPIRIT", "ASGARD", "LEGENDARY",
     "speedster", "LARGE", "mv_sturmgesang",
     "Acht Beine, neun Welten, kein Weg zu weit.",
     "Eight legs, nine worlds, no road too far."),
    ("helvarn", "Helvarn", "Helvarn", "SHADOW", "DIVINE", "HELHEIM", "MYTHIC",
     "wall", "LARGE", "mv_langnacht",
     "Sie herrscht über alle, die nicht im Kampf fielen — also über fast alle.",
     "She rules everyone who did not fall in battle — which is nearly everyone."),
    ("odrmund", "Odrmund", "Odrmund", "DIVINE", "RUNE", "ASGARD", "MYTHIC",
     "mage", "LARGE", "mv_asenurteil",
     "Ein Auge gab es fort. Mit dem anderen sieht es zu viel.",
     "One eye it gave away. With the other it sees far too much."),
    ("nidravel", "Nidravel", "Nidravel", "CHAOS", None, "HELHEIM", "MYTHIC",
     "glass", "HUGE", "mv_ragnaroek",
     "Es ist kein Wesen des Chaos. Es ist das Chaos, das gelernt hat, ein Wesen zu sein.",
     "It is not a creature of chaos. It is chaos that learned to be a creature."),
    ("yggmund", "Yggmund", "Yggmund", "NATURE", "DIVINE", "MIDGARD", "DIVINE",
     "wall", "COLOSSAL", "mv_lebenssaat",
     "Ein Ableger des Weltenbaums, der beschloss, zu gehen statt zu stehen.",
     "A scion of the World Tree that decided to walk instead of stand."),
    ("urdverd", "Urdverd", "Urdverd", "RUNE", "SPIRIT", "MIDGARD", "DIVINE",
     "support", "MEDIUM", "mv_erstes_zeichen",
     "Drei Stimmen, ein Wesen: was war, was ist, was werden muss.",
     "Three voices, one being: what was, what is, what must be."),
]

# Which ability pool fits which role.
ROLE_ABILITIES = {
    "attacker": ["ab_brute_force", "ab_berserker", "ab_keen_eye", "ab_swift_strike"],
    "mage": ["ab_arcane_focus", "ab_spirit_veil", "ab_rune_siphon", "ab_clear_mind"],
    "bruiser": ["ab_brute_force", "ab_thorn_mail", "ab_last_stand", "ab_bulwark"],
    "tank": ["ab_bulwark", "ab_giant_frame", "ab_runic_bulwark", "ab_regrowth"],
    "wall": ["ab_spirit_veil", "ab_rune_ward", "ab_regrowth", "ab_iron_will"],
    "speedster": ["ab_gale_step", "ab_keen_eye", "ab_swift_strike", "ab_lucky_star"],
    "support": ["ab_cleansing_light", "ab_regrowth", "ab_lucky_star", "ab_divine_grace"],
    "glass": ["ab_arcane_focus", "ab_berserker", "ab_soul_siphon", "ab_last_stand"],
    "balanced": ["ab_iron_will", "ab_keen_eye", "ab_regrowth", "ab_bulwark"],
}

ELEMENT_ABILITIES = {
    "FIRE": "ab_ember_touch", "ICE": "ab_frost_touch", "NATURE": "ab_venom_touch",
    "SHADOW": "ab_curse_touch", "THUNDER": "ab_storm_born", "WATER": "ab_call_rain",
    "EARTH": "ab_stone_hide", "WIND": "ab_gale_hide", "METAL": "ab_bulwark",
    "SPIRIT": "ab_moon_drinker", "LIGHT": "ab_cleansing_light", "RUNE": "ab_rune_ward",
    "CHAOS": "ab_chaos_heart", "DIVINE": "ab_divine_grace",
}

SIZE_HEIGHT = {
    "TINY": (18, 45), "SMALL": (50, 95), "MEDIUM": (100, 175),
    "LARGE": (180, 290), "HUGE": (300, 620), "COLOSSAL": (700, 2400),
}


def _learnset(moves_by_element: dict, element: str, secondary, role: str, stage: int,
              roll, signature: str | None = None) -> list[dict]:
    """Builds a level-up learnset from the generated move pool."""
    primary_pool = moves_by_element[element]
    secondary_pool = moves_by_element.get(secondary, []) if secondary else []
    magical = role in ("mage", "glass", "support")

    def pick(pool, category, tier, fallback_any=True):
        options = [m for m in pool if m["tier"] == tier and
                   (m["category"] == category or (category is None))]
        if not options and fallback_any:
            options = [m for m in pool if m["tier"] == tier]
        return roll.choice(options)["id"] if options else None

    schedule = [
        (1, primary_pool, "PHYSICAL" if not magical else "MAGICAL", 1),
        (4, primary_pool, "SUPPORT", 2),
        (8, primary_pool, "PHYSICAL" if not magical else "MAGICAL", 1),
        (12, secondary_pool or primary_pool, None, 2),
        (16, primary_pool, "PHYSICAL" if not magical else "MAGICAL", 2),
        (21, primary_pool, "SUPPORT", 2),
        (26, secondary_pool or primary_pool, None, 2),
        (31, primary_pool, "PHYSICAL" if not magical else "MAGICAL", 3),
        (37, primary_pool, "SUPPORT", 3),
        (43, secondary_pool or primary_pool, None, 3),
        (50, primary_pool, "PHYSICAL" if not magical else "MAGICAL", 3),
        (58, primary_pool, None, 3),
        (66, primary_pool, "PHYSICAL" if not magical else "MAGICAL", 4),
    ]
    entries = []
    used = set()
    for level, pool, category, tier in schedule:
        # Higher stages learn their late moves earlier.
        adjusted = max(1, int(level * (1.0 - 0.12 * (stage - 1))))
        move_id = pick(pool, category, tier)
        if move_id and move_id not in used:
            used.add(move_id)
            entries.append({"level": adjusted, "moveId": move_id})
    if signature:
        entries.append({"level": 1, "moveId": signature})
    entries.sort(key=lambda e: e["level"])
    return entries


def _rarity_for_stage(base_rarity: str, stage: int, stage_count: int) -> str:
    order = ["COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "DIVINE"]
    index = order.index(base_rarity)
    if stage == stage_count and stage_count > 1:
        index = min(index + 1, len(order) - 1)
    return order[index]


def build_monsters(strings: Strings, moves: list[dict]) -> list[dict]:
    roll = rng("monsters")
    moves_by_element: dict[str, list[dict]] = {}
    for move in moves:
        if move["tier"] <= 4:
            moves_by_element.setdefault(move["element"], []).append(move)

    species: list[dict] = []
    dex = 1

    for (family_id, names, primary, secondary, world, rarity, role,
         egg_groups, size, growth, lore_de, lore_en) in FAMILIES:
        stage_count = len(names)
        for stage_index, name_de in enumerate(names, start=1):
            species_id = slug(name_de)
            stage_rarity = _rarity_for_stage(rarity, stage_index, stage_count)
            stats = stat_block(role, stage_index, stage_rarity, roll)
            height_low, height_high = SIZE_HEIGHT[size]
            height = int(height_low + (height_high - height_low) * (stage_index / stage_count))

            abilities = list(dict.fromkeys(
                ROLE_ABILITIES[role][:2] + [ELEMENT_ABILITIES[primary]]
            ))
            hidden = ELEMENT_ABILITIES.get(secondary) if secondary else ROLE_ABILITIES[role][-1]

            evolutions = []
            if stage_index < stage_count:
                target = slug(names[stage_index])
                evolutions.append(_evolution_for(family_id, stage_index, target, primary,
                                                 secondary, world, roll, strings))

            strings.add(f"species_{species_id}_name", name_de, _english_name(name_de, names, stage_index))
            strings.add(
                f"species_{species_id}_desc",
                _description_de(name_de, primary, secondary, stage_index, stage_count, role),
                _description_en(primary, secondary, stage_index, stage_count, role),
            )
            strings.add(
                f"species_{species_id}_lore",
                f"{lore_de} {_stage_clause_de(stage_index, stage_count)}",
                f"{lore_en} {_stage_clause_en(stage_index, stage_count)}",
            )

            species.append({
                "id": species_id,
                "dexNumber": dex,
                "nameKey": f"species_{species_id}_name",
                "descriptionKey": f"species_{species_id}_desc",
                "loreKey": f"species_{species_id}_lore",
                "primaryElement": primary,
                "secondaryElement": secondary,
                "familyId": family_id,
                "stage": stage_index,
                "baseStats": stats,
                "rarity": stage_rarity,
                "growthRate": growth,
                "catchRate": catch_rate(stage_rarity, stage_index),
                "baseExperience": base_experience(stage_index, stage_rarity),
                "sizeClass": size,
                "heightCm": height,
                "weightHg": int(height * roll.uniform(2.4, 7.5)),
                "nativeWorld": world,
                "habitats": [world.lower()],
                "genderRatio": "GENDERLESS" if "construct" in egg_groups else "BALANCED",
                "eggGroups": egg_groups,
                "eggCycles": 12 + stage_count * 4,
                "abilityIds": abilities,
                "hiddenAbilityId": hidden,
                "talentTreeId": f"talent_{role}",
                "learnset": _learnset(moves_by_element, primary, secondary, role, stage_index, roll),
                "tutorMoveIds": _tutor_moves(moves_by_element, primary, secondary, roll),
                "eggMoveIds": _egg_moves(moves_by_element, primary, roll),
                "evolutions": evolutions,
                "spriteKey": f"spr_{species_id}",
                "cryKey": f"cry_{family_id}",
                "shinyPaletteKey": f"pal_{species_id}_shiny",
                "bestiaryFlavourKeys": [f"species_{species_id}_lore"],
            })
            dex += 1

    for (legend_id, name_de, name_en, primary, secondary, world, rarity, role, size,
         signature, lore_de, lore_en) in LEGENDS:
        stats = stat_block(role, 3, rarity, roll)
        height_low, height_high = SIZE_HEIGHT[size]
        strings.add(f"species_{legend_id}_name", name_de, name_en)
        strings.add(
            f"species_{legend_id}_desc",
            f"Ein legendäres Wesen aus {world.title()}. Sein Erscheinen verändert das Wetter der ganzen Region.",
            f"A legendary being from {world.title()}. Its arrival changes the weather of an entire region.",
        )
        strings.add(f"species_{legend_id}_lore", lore_de, lore_en)
        species.append({
            "id": legend_id,
            "dexNumber": dex,
            "nameKey": f"species_{legend_id}_name",
            "descriptionKey": f"species_{legend_id}_desc",
            "loreKey": f"species_{legend_id}_lore",
            "primaryElement": primary,
            "secondaryElement": secondary,
            "familyId": f"{legend_id}_solitary",
            "stage": 1,
            "baseStats": stats,
            "rarity": rarity,
            "growthRate": "GLACIAL",
            "catchRate": catch_rate(rarity, 1),
            "baseExperience": base_experience(3, rarity),
            "sizeClass": size,
            "heightCm": height_high,
            "weightHg": height_high * 12,
            "nativeWorld": world,
            "habitats": [world.lower(), "sanctuary"],
            "genderRatio": "GENDERLESS",
            "eggGroups": [],
            "eggCycles": 60,
            "abilityIds": ROLE_ABILITIES[role][:2],
            "hiddenAbilityId": ELEMENT_ABILITIES[primary],
            "talentTreeId": f"talent_{role}",
            "learnset": _learnset(moves_by_element, primary, secondary, role, 3, roll, signature),
            "tutorMoveIds": [],
            "eggMoveIds": [],
            "evolutions": [],
            "spriteKey": f"spr_{legend_id}",
            "cryKey": f"cry_{legend_id}",
            "shinyPaletteKey": f"pal_{legend_id}_shiny",
            "bestiaryFlavourKeys": [f"species_{legend_id}_lore"],
        })
        dex += 1

    return species


def _english_name(name_de: str, names: list[str], stage: int) -> str:
    """A readable English rendering; the German coinage stays the canonical one."""
    table = {
        "wicht": "wight", "welpe": "pup", "fang": "fang", "jarl": "jarl", "geist": "spirit",
        "herr": "lord", "fürst": "prince", "wacht": "watch", "wächter": "warden",
        "könig": "king", "königin": "queen", "mutter": "mother", "kraehe": "crow",
        "krähe": "crow", "rabe": "raven", "hund": "hound", "wolf": "wolf",
        "adler": "eagle", "eule": "owl", "kauz": "owlet", "falter": "moth",
    }
    lowered = name_de.lower()
    for german, english in table.items():
        if lowered.endswith(german):
            return f"{name_de[: -len(german)]}{english}".title()
    return name_de


def _description_de(name: str, primary: str, secondary, stage: int, stages: int, role: str) -> str:
    typing = ELEMENT_DE[primary] + (f"/{ELEMENT_DE[secondary]}" if secondary else "")
    role_de = {
        "attacker": "greift schnell und hart an", "mage": "kämpft mit magischer Wucht",
        "bruiser": "hält viel aus und teilt kräftig aus", "tank": "bildet eine lebende Mauer",
        "wall": "wehrt magische Angriffe ab", "speedster": "handelt fast immer zuerst",
        "support": "stärkt seine Verbündeten", "glass": "richtet enormen Schaden an, hält aber wenig aus",
        "balanced": "ist in allem solide",
    }[role]
    stufe = "Grundform" if stage == 1 else ("Endform" if stage == stages else "Zwischenform")
    return f"{typing}-Wesen ({stufe}). Es {role_de}."


def _description_en(primary: str, secondary, stage: int, stages: int, role: str) -> str:
    typing = ELEMENT_EN[primary] + (f"/{ELEMENT_EN[secondary]}" if secondary else "")
    role_en = {
        "attacker": "strikes fast and hard", "mage": "fights with magical force",
        "bruiser": "endures much and hits back harder", "tank": "forms a living wall",
        "wall": "turns aside magical attacks", "speedster": "almost always acts first",
        "support": "strengthens its allies", "glass": "deals enormous damage but breaks easily",
        "balanced": "is solid at everything",
    }[role]
    stage_word = "base form" if stage == 1 else ("final form" if stage == stages else "middle form")
    return f"A {typing} creature ({stage_word}). It {role_en}."


def _stage_clause_de(stage: int, stages: int) -> str:
    if stages == 1:
        return "Es steht allein — es gab nie mehr als eines seiner Art."
    if stage == 1:
        return "In dieser Form ist es noch neugierig und leicht zu gewinnen."
    if stage == stages:
        return "In seiner letzten Gestalt erinnert es sich an alles, was es je war."
    return "Auf halbem Weg zwischen dem, was es war, und dem, was es wird."


def _stage_clause_en(stage: int, stages: int) -> str:
    if stages == 1:
        return "It stands alone — there was never more than one of its kind."
    if stage == 1:
        return "In this form it is still curious and easily befriended."
    if stage == stages:
        return "In its final shape it remembers everything it has ever been."
    return "Halfway between what it was and what it will become."


def _evolution_for(family_id, stage_index, target, primary, secondary, world, roll, strings) -> dict:
    """Picks a thematically fitting evolution condition.

    Most lines evolve by level; roughly a third use one of the deeper triggers
    so that the Bestiarium stays interesting to complete.
    """
    key = f"evo_{family_id}_{stage_index}"
    kind = roll.random()
    base_level = 16 + stage_index * 14

    if kind < 0.60:
        strings.add(key, f"Entwickelt sich ab Stufe {base_level}.",
                    f"Evolves at level {base_level}.")
        return {"targetSpeciesId": target, "trigger": "LEVEL_UP", "minLevel": base_level,
                "descriptionKey": key}
    if kind < 0.70:
        strings.add(key, "Entwickelt sich bei hoher Freundschaft.",
                    "Evolves at high friendship.")
        return {"targetSpeciesId": target, "trigger": "FRIENDSHIP", "minFriendship": 220,
                "descriptionKey": key}
    if kind < 0.80:
        stone = f"stein_{ {'FIRE': 'glut', 'WATER': 'flut', 'ICE': 'frost', 'WIND': 'sturm', 'EARTH': 'fels', 'NATURE': 'hain', 'THUNDER': 'blitz', 'LIGHT': 'glanz', 'SHADOW': 'schatten', 'SPIRIT': 'geist', 'RUNE': 'rune', 'CHAOS': 'chaos', 'METAL': 'erz', 'DIVINE': 'asen'}[primary] }"
        strings.add(key, "Entwickelt sich durch einen passenden Entwicklungsstein.",
                    "Evolves when the matching evolution stone is used.")
        return {"targetSpeciesId": target, "trigger": "USE_ITEM", "requiredItemId": stone,
                "consumesItem": True, "descriptionKey": key}
    if kind < 0.88:
        phase = roll.choice(["NIGHT", "DAY"])
        phase_de = "nachts" if phase == "NIGHT" else "tagsüber"
        strings.add(key, f"Entwickelt sich ab Stufe {base_level} {phase_de}.",
                    f"Evolves at level {base_level} during the {phase.lower()}.")
        return {"targetSpeciesId": target, "trigger": "LEVEL_UP", "minLevel": base_level,
                "requiredTimeOfDay": phase, "descriptionKey": key}
    if kind < 0.94:
        strings.add(key, f"Entwickelt sich ab Stufe {base_level} in seiner Heimatwelt.",
                    f"Evolves at level {base_level} in its home world.")
        return {"targetSpeciesId": target, "trigger": "LEVEL_UP", "minLevel": base_level,
                "requiredWorld": world, "descriptionKey": key}
    strings.add(key, "Entwickelt sich, wenn eine passende Rune gebunden ist.",
                "Evolves when a matching rune is bound to it.")
    return {"targetSpeciesId": target, "trigger": "RUNE_BINDING",
            "requiredRuneId": f"rune_{primary.lower()}", "minLevel": base_level,
            "descriptionKey": key}


def _tutor_moves(moves_by_element, primary, secondary, roll) -> list[str]:
    pool = moves_by_element[primary] + (moves_by_element.get(secondary, []) if secondary else [])
    universal = ["mv_heilende_hand", "mv_runenschirm", "mv_reinigende_flamme", "mv_seherblick"]
    picks = roll.sample(pool, min(4, len(pool)))
    return [move["id"] for move in picks] + universal


def _egg_moves(moves_by_element, primary, roll) -> list[str]:
    pool = [m for m in moves_by_element[primary] if m["tier"] >= 3]
    if not pool:
        return []
    return [move["id"] for move in roll.sample(pool, min(3, len(pool)))]
