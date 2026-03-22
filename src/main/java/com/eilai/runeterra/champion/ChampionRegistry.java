package com.eilai.runeterra.champion;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry of every League of Legends champion.
 *
 * All champions are UNDER_CONSTRUCTION by default.
 * To mark a champion as ready, call .available() and .splash("filename.png")
 * in their builder entry, and fill in their ability descriptions.
 *
 * The "no_champion" entry is always AVAILABLE and appears as the first card.
 */
public final class ChampionRegistry {

    private static final Map<String, ChampionDefinition> BY_ID = new LinkedHashMap<>();
    private static final List<ChampionDefinition> ALL;

    static {
        // ── Special: No Champion (always available, always first) ─────────────
        register(ChampionDefinition.builder("no_champion", "No Champion", "Play as yourself")
                .available()
                .splash("no_champion.png")
                .passive("No passive. You are just a regular player.")
                .q("No ability.")
                .w("No ability.")
                .e("No ability.")
                .r("No ability.")
                .d("No ability.")
                .f("No ability.")
                .build());

        // ── A ─────────────────────────────────────────────────────────────────
        register(champ("ahri",          "Ahri",          "the Nine-Tailed Fox"));
        register(champ("akali",         "Akali",         "the Rogue Assassin"));
        register(champ("akshan",        "Akshan",        "the Rogue Sentinel"));
        register(champ("alistar",       "Alistar",       "the Minotaur"));
        register(champ("amumu",         "Amumu",         "the Sad Mummy"));
        register(champ("anivia",        "Anivia",        "the Cryophoenix"));
        register(champ("annie",         "Annie",         "the Dark Child"));
        register(champ("aphelios",      "Aphelios",      "the Weapon of the Faithful"));
        register(champ("ashe",          "Ashe",          "the Frost Archer"));
        register(champ("aurelion_sol",  "Aurelion Sol",  "the Star Forger"));
        register(champ("aurora",        "Aurora",        "the Wild Mage"));
        register(champ("azir",          "Azir",          "the Emperor of the Sands"));

        // ── B ─────────────────────────────────────────────────────────────────
        register(champ("bard",          "Bard",          "the Wandering Caretaker"));
        register(champ("belveth",       "Bel'Veth",      "the Empress of the Void"));
        register(champ("blitzcrank",    "Blitzcrank",    "the Great Steam Golem"));
        register(champ("brand",         "Brand",         "the Burning Vengeance"));
        register(champ("braum",         "Braum",         "the Heart of the Freljord"));
        register(champ("briar",         "Briar",         "the Restrained Hunger"));

        // ── C ─────────────────────────────────────────────────────────────────
        register(champ("caitlyn",       "Caitlyn",       "the Sheriff of Piltover"));
        register(champ("camille",       "Camille",       "the Steel Shadow"));
        register(champ("cassiopeia",    "Cassiopeia",    "the Serpent's Embrace"));
        register(champ("chogath",       "Cho'Gath",      "the Terror of the Void"));
        register(champ("corki",         "Corki",         "the Daring Bombardier"));

        // ── D ─────────────────────────────────────────────────────────────────
        register(champ("darius",        "Darius",        "the Hand of Noxus"));
        register(champ("diana",         "Diana",         "Scorn of the Moon"));
        register(champ("draven",        "Draven",        "the Glorious Executioner"));
        register(champ("drmundo",       "Dr. Mundo",     "the Madman of Zaun"));

        // ── E ─────────────────────────────────────────────────────────────────
        register(champ("ekko",          "Ekko",          "the Boy Who Shattered Time"));
        register(champ("elise",         "Elise",         "the Spider Queen"));
        register(champ("evelynn",       "Evelynn",       "Agony's Embrace"));
        register(champ("ezreal",        "Ezreal",        "the Prodigal Explorer"));

        // ── F ─────────────────────────────────────────────────────────────────
        register(champ("fiddlesticks",  "Fiddlesticks",  "the Ancient Fear"));
        register(champ("fiora",         "Fiora",         "the Grand Duelist"));
        register(champ("fizz",          "Fizz",          "the Tidal Trickster"));

        // ── G ─────────────────────────────────────────────────────────────────
        register(champ("galio",         "Galio",         "the Colossus"));
        register(champ("gangplank",     "Gangplank",     "the Saltwater Scourge"));
        register(champ("garen",         "Garen",         "the Might of Demacia"));
        register(champ("gnar",          "Gnar",          "the Missing Link"));
        register(champ("gragas",        "Gragas",        "the Rabble Rouser"));
        register(champ("graves",        "Graves",        "the Outlaw"));
        register(champ("gwen",          "Gwen",          "the Hallowed Seamstress"));

        // ── H ─────────────────────────────────────────────────────────────────
        register(champ("hecarim",       "Hecarim",       "the Shadow of War"));
        register(champ("heimerdinger",  "Heimerdinger",  "the Revered Inventor"));
        register(champ("hwei",          "Hwei",          "the Visionary"));

        // ── I ─────────────────────────────────────────────────────────────────
        register(champ("illaoi",        "Illaoi",        "the Kraken Priestess"));
        register(champ("irelia",        "Irelia",        "the Blade Dancer"));
        register(champ("ivern",         "Ivern",         "the Green Father"));

        // ── J ─────────────────────────────────────────────────────────────────
        register(champ("janna",         "Janna",         "the Storm's Fury"));
        register(champ("jarvaniv",      "Jarvan IV",     "the Exemplar of Demacia"));
        register(champ("jax",           "Jax",           "Grandmaster at Arms"));
        register(champ("jayce",         "Jayce",         "the Defender of Tomorrow"));
        register(champ("jhin",          "Jhin",          "the Virtuoso"));
        register(champ("jinx",          "Jinx",          "the Loose Cannon"));

        // ── K ─────────────────────────────────────────────────────────────────
        register(champ("kaisa",         "Kai'Sa",        "Daughter of the Void"));
        register(champ("kalista",       "Kalista",       "the Spear of Vengeance"));
        register(champ("karma",         "Karma",         "the Enlightened One"));
        register(champ("karthus",       "Karthus",       "the Deathsinger"));
        register(champ("kassadin",      "Kassadin",      "the Void Walker"));
        register(champ("katarina",      "Katarina",      "the Sinister Blade"));
        register(champ("kayle",         "Kayle",         "the Righteous"));
        register(champ("kayn",          "Kayn",          "the Shadow Reaper"));
        register(champ("kennen",        "Kennen",        "the Heart of the Tempest"));
        register(champ("khazix",        "Kha'Zix",       "the Voidreaver"));
        register(champ("kindred",       "Kindred",       "the Eternal Hunters"));
        register(champ("kled",          "Kled",          "the Cantankerous Cavalier"));
        register(champ("kogmaw",        "Kog'Maw",       "the Mouth of the Abyss"));
        register(champ("ksante",        "K'Sante",       "the Pride of Nazumah"));

        // ── L ─────────────────────────────────────────────────────────────────
        register(champ("leblanc",       "LeBlanc",       "the Deceiver"));
        register(champ("leesin",        "Lee Sin",       "the Blind Monk"));
        register(champ("leona",         "Leona",         "the Radiant Dawn"));
        register(champ("lillia",        "Lillia",        "the Bashful Bloom"));
        register(champ("lissandra",     "Lissandra",     "the Ice Witch"));
        register(champ("lucian",        "Lucian",        "the Purifier"));
        register(champ("lulu",          "Lulu",          "the Fae Sorceress"));
        register(champ("lux",           "Lux",           "the Lady of Luminosity"));

        // ── M ─────────────────────────────────────────────────────────────────
        register(champ("malphite",      "Malphite",      "Shard of the Monolith"));
        register(champ("malzahar",      "Malzahar",      "the Prophet of the Void"));
        register(champ("maokai",        "Maokai",        "the Twisted Treant"));
        register(champ("masteryi",      "Master Yi",     "the Wuju Bladesman"));
        register(champ("milio",         "Milio",         "the Gentle Flame"));
        register(champ("missfortune",   "Miss Fortune",  "the Bounty Hunter"));
        register(champ("mordekaiser",   "Mordekaiser",   "the Iron Revenant"));
        register(champ("morgana",       "Morgana",       "the Fallen"));

        // ── N ─────────────────────────────────────────────────────────────────
        register(champ("naafiri",       "Naafiri",       "the Hound of a Hundred Bites"));
        register(champ("nami",          "Nami",          "the Tidecaller"));
        register(champ("nasus",         "Nasus",         "the Curator of the Sands"));
        register(champ("nautilus",      "Nautilus",      "the Titan of the Depths"));
        register(champ("neeko",         "Neeko",         "the Curious Chameleon"));
        register(champ("nidalee",       "Nidalee",       "the Bestial Huntress"));
        register(champ("nilah",         "Nilah",         "the Joy Unbound"));
        register(champ("nocturne",      "Nocturne",      "the Eternal Nightmare"));
        register(champ("nunu",          "Nunu & Willump", "the Boy and His Yeti"));

        // ── O ─────────────────────────────────────────────────────────────────
        register(champ("olaf",          "Olaf",          "the Berserker"));
        register(champ("orianna",       "Orianna",       "the Lady of Clockwork"));
        register(champ("ornn",          "Ornn",          "the Fire below the Mountain"));

        // ── P ─────────────────────────────────────────────────────────────────
        register(champ("pantheon",      "Pantheon",      "the Unbreakable Spear"));
        register(champ("poppy",         "Poppy",         "Keeper of the Hammer"));
        register(champ("pyke",          "Pyke",          "the Bloodharbor Ripper"));

        // ── Q ─────────────────────────────────────────────────────────────────
        register(champ("qiyana",        "Qiyana",        "Empress of the Elements"));
        register(champ("quinn",         "Quinn",         "Demacia's Wings"));

        // ── R ─────────────────────────────────────────────────────────────────
        register(champ("rakan",         "Rakan",         "the Charmer"));
        register(champ("rammus",        "Rammus",        "the Armordillo"));
        register(champ("reksai",        "Rek'Sai",       "the Void Burrower"));
        register(champ("rell",          "Rell",          "the Iron Maiden"));
        register(champ("renata",        "Renata Glasc",  "the Chem-Baroness"));
        register(champ("renekton",      "Renekton",      "the Butcher of the Sands"));
        register(champ("rengar",        "Rengar",        "the Pridestalker"));
        register(champ("riven",         "Riven",         "the Exile"));
        register(champ("rumble",        "Rumble",        "the Mechanized Menace"));
        register(champ("ryze",          "Ryze",          "the Rune Mage"));

        // ── S ─────────────────────────────────────────────────────────────────
        register(champ("samira",        "Samira",        "the Desert Rose"));
        register(champ("sejuani",       "Sejuani",       "Fury of the North"));
        register(champ("senna",         "Senna",         "the Redeemer"));
        register(champ("seraphine",     "Seraphine",     "the Starry-Eyed Songstress"));
        register(champ("sett",          "Sett",          "the Boss"));
        register(champ("shaco",         "Shaco",         "the Demon Jester"));
        register(champ("shen",          "Shen",          "the Eye of Twilight"));
        register(champ("shyvana",       "Shyvana",       "the Half-Dragon"));
        register(champ("singed",        "Singed",        "the Mad Chemist"));
        register(champ("sion",          "Sion",          "the Undead Juggernaut"));
        register(champ("sivir",         "Sivir",         "the Battle Mistress"));
        register(champ("skarner",       "Skarner",       "the Primordial Sovereign"));
        register(champ("smolder",       "Smolder",       "the Crown Prince of Dragons"));
        register(champ("sona",          "Sona",          "Maven of the Strings"));
        register(champ("soraka",        "Soraka",        "the Starchild"));
        register(champ("swain",         "Swain",         "the Noxian Grand General"));
        register(champ("sylas",         "Sylas",         "the Unshackled"));
        register(champ("syndra",        "Syndra",        "the Dark Sovereign"));

        // ── T ─────────────────────────────────────────────────────────────────
        register(champ("tahmkench",     "Tahm Kench",    "the River King"));
        register(champ("taliyah",       "Taliyah",       "the Stoneweaver"));
        register(champ("talon",         "Talon",         "the Blade's Shadow"));
        register(champ("taric",         "Taric",         "the Shield of Valoran"));
        register(champ("teemo",         "Teemo",         "the Swift Scout"));
        register(champ("thresh",        "Thresh",        "the Chain Warden"));
        register(champ("tristana",      "Tristana",      "the Yordle Gunner"));
        register(champ("trundle",       "Trundle",       "the Troll King"));
        register(champ("tryndamere",    "Tryndamere",    "the Barbarian King"));
        register(champ("twistedfate",   "Twisted Fate",  "the Card Master"));
        register(champ("twitch",        "Twitch",        "the Plague Rat"));

        // ── U ─────────────────────────────────────────────────────────────────
        register(champ("udyr",          "Udyr",          "the Spirit Walker"));
        register(champ("urgot",         "Urgot",         "the Dreadnought"));

        // ── V ─────────────────────────────────────────────────────────────────
        register(champ("varus",         "Varus",         "the Arrow of Retribution"));
        register(champ("vayne",         "Vayne",         "the Night Hunter"));
        register(champ("veigar",        "Veigar",        "the Tiny Master of Evil"));
        register(champ("velkoz",        "Vel'Koz",       "the Eye of the Void"));
        register(champ("vex",           "Vex",           "the Gloomist"));
        register(champ("vi",            "Vi",            "the Piltover Enforcer"));
        register(champ("viego",         "Viego",         "the Ruined King"));
        register(champ("viktor",        "Viktor",        "the Machine Herald"));
        register(champ("vladimir",      "Vladimir",      "the Crimson Reaper"));
        register(champ("volibear",      "Volibear",      "the Relentless Storm"));

        // ── W ─────────────────────────────────────────────────────────────────
        register(champ("warwick",       "Warwick",       "the Uncaged Wrath of Zaun"));
        register(champ("wukong",        "Wukong",        "the Monkey King"));

        // ── X ─────────────────────────────────────────────────────────────────
        register(champ("xayah",         "Xayah",         "the Rebel"));
        register(champ("xerath",        "Xerath",        "the Magus Ascendant"));
        register(champ("xinzhao",       "Xin Zhao",      "the Seneschal of Demacia"));

        // ── Y ─────────────────────────────────────────────────────────────────
        register(champ("yasuo",         "Yasuo",         "the Unforgiven"));
        register(champ("yone",          "Yone",          "the Unforgotten"));
        register(champ("yorick",        "Yorick",        "Shepherd of Souls"));
        register(champ("yuumi",         "Yuumi",         "the Magical Cat"));

        // ── Z ─────────────────────────────────────────────────────────────────
        register(champ("zac",           "Zac",           "the Secret Weapon"));
        register(champ("zed",           "Zed",           "the Master of Shadows"));
        register(champ("zeri",          "Zeri",          "the Spark of Zaun"));
        register(champ("ziggs",         "Ziggs",         "the Hexplosives Expert"));
        register(champ("zilean",        "Zilean",        "the Chronokeeper"));
        register(champ("zoe",           "Zoe",           "the Aspect of Twilight"));
        register(champ("zyra",          "Zyra",          "Rise of the Thorns"));
        register(champ("zaahen",        "Zaahen",        "the Unsundered"));

        ALL = Collections.unmodifiableList(List.copyOf(BY_ID.values()));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void register(ChampionDefinition def) {
        BY_ID.put(def.id(), def);
    }

    /** Shorthand: build an UNDER_CONSTRUCTION champion with no abilities filled in. */
    private static ChampionDefinition champ(String id, String name, String title) {
        return ChampionDefinition.builder(id, name, title).build();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** All champions in insertion order (No Champion first, then alphabetical). */
    public static List<ChampionDefinition> all() { return ALL; }

    /** All champions excluding the "no_champion" entry. */
    public static List<ChampionDefinition> allPlayable() {
        return ALL.stream().filter(c -> !c.id().equals("no_champion")).toList();
    }

    /** Look up a champion by id. Returns empty if not found. */
    public static Optional<ChampionDefinition> get(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** The special No Champion card, always available. */
    public static ChampionDefinition noChampion() {
        return BY_ID.get("no_champion");
    }
}
