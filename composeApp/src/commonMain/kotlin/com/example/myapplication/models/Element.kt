package com.example.myapplication.models

enum class ElementCategory(val colorHex: Long) {
    ALKALI_METAL(0xFFFF8A65),
    ALKALINE_EARTH(0xFFFFCC80),
    TRANSITION_METAL(0xFF90CAF9),
    POST_TRANSITION(0xFF80CBC4),
    METALLOID(0xFFA5D6A7),
    REACTIVE_NONMETAL(0xFFFFF176),
    HALOGEN(0xFFDCE775),
    NOBLE_GAS(0xFFCE93D8),
    LANTHANIDE(0xFFF48FB1),
    ACTINIDE(0xFFFFAB91),
    UNKNOWN(0xFFB0BEC5)
}

/**
 * @param tableRow  1–7 = normal periods, 8 = lanthanides row, 9 = actinides row
 * @param tableCol  1–18 for main block, 4–17 for f-block rows
 */
data class Element(
    val atomicNumber: Int,
    val symbol: String,
    val namePL: String,
    val atomicMass: Float,
    val tableRow: Int,
    val tableCol: Int,
    val category: ElementCategory,
    val electronConfig: String = "",
    val groupName: String = ""
)

private val AM  = ElementCategory.ALKALI_METAL
private val AE  = ElementCategory.ALKALINE_EARTH
private val TM  = ElementCategory.TRANSITION_METAL
private val PT  = ElementCategory.POST_TRANSITION
private val ME  = ElementCategory.METALLOID
private val RN  = ElementCategory.REACTIVE_NONMETAL
private val HA  = ElementCategory.HALOGEN
private val NG  = ElementCategory.NOBLE_GAS
private val LAN = ElementCategory.LANTHANIDE
private val ACT = ElementCategory.ACTINIDE
private val UNK = ElementCategory.UNKNOWN

val ELEMENTS: List<Element> = listOf(
    // Period 1
    Element(1,  "H",  "Wodór",     1.008f,  1,  1, RN,  "1s¹",              ""),
    Element(2,  "He", "Hel",       4.003f,  1, 18, NG,  "1s²",              "Gazy szlachetne"),
    // Period 2
    Element(3,  "Li", "Lit",       6.941f,  2,  1, AM,  "[He] 2s¹",         "Litowce"),
    Element(4,  "Be", "Beryl",     9.012f,  2,  2, AE,  "[He] 2s²",         "Berylowce"),
    Element(5,  "B",  "Bor",      10.811f,  2, 13, ME,  "[He] 2s² 2p¹",     "Borowce"),
    Element(6,  "C",  "Węgiel",   12.011f,  2, 14, RN,  "[He] 2s² 2p²",     "Węglowce"),
    Element(7,  "N",  "Azot",     14.007f,  2, 15, RN,  "[He] 2s² 2p³",     "Azotowce"),
    Element(8,  "O",  "Tlen",     15.999f,  2, 16, RN,  "[He] 2s² 2p⁴",     "Tlenowce"),
    Element(9,  "F",  "Fluor",    18.998f,  2, 17, HA,  "[He] 2s² 2p⁵",     "Halogeny"),
    Element(10, "Ne", "Neon",     20.180f,  2, 18, NG,  "[He] 2s² 2p⁶",     "Gazy szlachetne"),
    // Period 3
    Element(11, "Na", "Sód",      22.990f,  3,  1, AM,  "[Ne] 3s¹",         "Litowce"),
    Element(12, "Mg", "Magnez",   24.305f,  3,  2, AE,  "[Ne] 3s²",         "Berylowce"),
    Element(13, "Al", "Glin",     26.982f,  3, 13, PT,  "[Ne] 3s² 3p¹",     "Borowce"),
    Element(14, "Si", "Krzem",    28.086f,  3, 14, ME,  "[Ne] 3s² 3p²",     "Węglowce"),
    Element(15, "P",  "Fosfor",   30.974f,  3, 15, RN,  "[Ne] 3s² 3p³",     "Azotowce"),
    Element(16, "S",  "Siarka",   32.065f,  3, 16, RN,  "[Ne] 3s² 3p⁴",     "Tlenowce"),
    Element(17, "Cl", "Chlor",    35.453f,  3, 17, HA,  "[Ne] 3s² 3p⁵",     "Halogeny"),
    Element(18, "Ar", "Argon",    39.948f,  3, 18, NG,  "[Ne] 3s² 3p⁶",     "Gazy szlachetne"),
    // Period 4
    Element(19, "K",  "Potas",    39.098f,  4,  1, AM,  "[Ar] 4s¹",         "Litowce"),
    Element(20, "Ca", "Wapń",     40.078f,  4,  2, AE,  "[Ar] 4s²",         "Berylowce"),
    Element(21, "Sc", "Skand",    44.956f,  4,  3, TM,  "[Ar] 3d¹ 4s²",     ""),
    Element(22, "Ti", "Tytan",    47.867f,  4,  4, TM,  "[Ar] 3d² 4s²",     ""),
    Element(23, "V",  "Wanad",    50.942f,  4,  5, TM,  "[Ar] 3d³ 4s²",     ""),
    Element(24, "Cr", "Chrom",    51.996f,  4,  6, TM,  "[Ar] 3d⁵ 4s¹",     ""),
    Element(25, "Mn", "Mangan",   54.938f,  4,  7, TM,  "[Ar] 3d⁵ 4s²",     ""),
    Element(26, "Fe", "Żelazo",   55.845f,  4,  8, TM,  "[Ar] 3d⁶ 4s²",     ""),
    Element(27, "Co", "Kobalt",   58.933f,  4,  9, TM,  "[Ar] 3d⁷ 4s²",     ""),
    Element(28, "Ni", "Nikiel",   58.693f,  4, 10, TM,  "[Ar] 3d⁸ 4s²",     ""),
    Element(29, "Cu", "Miedź",    63.546f,  4, 11, TM,  "[Ar] 3d¹⁰ 4s¹",    ""),
    Element(30, "Zn", "Cynk",     65.38f,   4, 12, TM,  "[Ar] 3d¹⁰ 4s²",    ""),
    Element(31, "Ga", "Gal",      69.723f,  4, 13, PT,  "[Ar] 3d¹⁰ 4s² 4p¹","Borowce"),
    Element(32, "Ge", "German",   72.630f,  4, 14, ME,  "[Ar] 3d¹⁰ 4s² 4p²","Węglowce"),
    Element(33, "As", "Arsen",    74.922f,  4, 15, ME,  "[Ar] 3d¹⁰ 4s² 4p³","Azotowce"),
    Element(34, "Se", "Selen",    78.971f,  4, 16, RN,  "[Ar] 3d¹⁰ 4s² 4p⁴","Tlenowce"),
    Element(35, "Br", "Brom",     79.904f,  4, 17, HA,  "[Ar] 3d¹⁰ 4s² 4p⁵","Halogeny"),
    Element(36, "Kr", "Krypton",  83.798f,  4, 18, NG,  "[Ar] 3d¹⁰ 4s² 4p⁶","Gazy szlachetne"),
    // Period 5
    Element(37, "Rb", "Rubid",    85.468f,  5,  1, AM,  "[Kr] 5s¹",         "Litowce"),
    Element(38, "Sr", "Stront",   87.62f,   5,  2, AE,  "[Kr] 5s²",         "Berylowce"),
    Element(39, "Y",  "Itr",      88.906f,  5,  3, TM,  "[Kr] 4d¹ 5s²",     ""),
    Element(40, "Zr", "Cyrkon",   91.224f,  5,  4, TM,  "[Kr] 4d² 5s²",     ""),
    Element(41, "Nb", "Niob",     92.906f,  5,  5, TM,  "[Kr] 4d⁴ 5s¹",     ""),
    Element(42, "Mo", "Molibden", 95.96f,   5,  6, TM,  "[Kr] 4d⁵ 5s¹",     ""),
    Element(43, "Tc", "Technet",  98.0f,    5,  7, TM,  "[Kr] 4d⁵ 5s²",     ""),
    Element(44, "Ru", "Ruten",   101.07f,   5,  8, TM,  "[Kr] 4d⁷ 5s¹",     ""),
    Element(45, "Rh", "Rod",     102.906f,  5,  9, TM,  "[Kr] 4d⁸ 5s¹",     ""),
    Element(46, "Pd", "Pallad",  106.42f,   5, 10, TM,  "[Kr] 4d¹⁰",        ""),
    Element(47, "Ag", "Srebro",  107.868f,  5, 11, TM,  "[Kr] 4d¹⁰ 5s¹",    ""),
    Element(48, "Cd", "Kadm",    112.411f,  5, 12, TM,  "[Kr] 4d¹⁰ 5s²",    ""),
    Element(49, "In", "Ind",     114.818f,  5, 13, PT,  "[Kr] 4d¹⁰ 5s² 5p¹","Borowce"),
    Element(50, "Sn", "Cyna",    118.710f,  5, 14, PT,  "[Kr] 4d¹⁰ 5s² 5p²","Węglowce"),
    Element(51, "Sb", "Antymon", 121.760f,  5, 15, ME,  "[Kr] 4d¹⁰ 5s² 5p³","Azotowce"),
    Element(52, "Te", "Tellur",  127.60f,   5, 16, ME,  "[Kr] 4d¹⁰ 5s² 5p⁴","Tlenowce"),
    Element(53, "I",  "Jod",     126.904f,  5, 17, HA,  "[Kr] 4d¹⁰ 5s² 5p⁵","Halogeny"),
    Element(54, "Xe", "Ksenon",  131.293f,  5, 18, NG,  "[Kr] 4d¹⁰ 5s² 5p⁶","Gazy szlachetne"),
    // Period 6
    Element(55, "Cs", "Cez",     132.905f,  6,  1, AM,  "[Xe] 6s¹",         "Litowce"),
    Element(56, "Ba", "Bar",     137.327f,  6,  2, AE,  "[Xe] 6s²",         "Berylowce"),
    Element(57, "La", "Lantan",  138.905f,  6,  3, LAN, "[Xe] 5d¹ 6s²",     "Lantanowce"),
    // Lanthanides — tableRow=8, tableCol=4..17
    Element(58, "Ce", "Cer",     140.116f,  8,  4, LAN, "[Xe] 4f¹ 5d¹ 6s²", "Lantanowce"),
    Element(59, "Pr", "Prazeodym",140.908f, 8,  5, LAN, "[Xe] 4f³ 6s²",     "Lantanowce"),
    Element(60, "Nd", "Neodym",  144.242f,  8,  6, LAN, "[Xe] 4f⁴ 6s²",     "Lantanowce"),
    Element(61, "Pm", "Promet",  145.0f,    8,  7, LAN, "[Xe] 4f⁵ 6s²",     "Lantanowce"),
    Element(62, "Sm", "Samar",   150.36f,   8,  8, LAN, "[Xe] 4f⁶ 6s²",     "Lantanowce"),
    Element(63, "Eu", "Europ",   151.964f,  8,  9, LAN, "[Xe] 4f⁷ 6s²",     "Lantanowce"),
    Element(64, "Gd", "Gadolin", 157.25f,   8, 10, LAN, "[Xe] 4f⁷ 5d¹ 6s²", "Lantanowce"),
    Element(65, "Tb", "Terb",    158.925f,  8, 11, LAN, "[Xe] 4f⁹ 6s²",     "Lantanowce"),
    Element(66, "Dy", "Dysproz", 162.500f,  8, 12, LAN, "[Xe] 4f¹⁰ 6s²",    "Lantanowce"),
    Element(67, "Ho", "Holm",    164.930f,  8, 13, LAN, "[Xe] 4f¹¹ 6s²",    "Lantanowce"),
    Element(68, "Er", "Erb",     167.259f,  8, 14, LAN, "[Xe] 4f¹² 6s²",    "Lantanowce"),
    Element(69, "Tm", "Tul",     168.934f,  8, 15, LAN, "[Xe] 4f¹³ 6s²",    "Lantanowce"),
    Element(70, "Yb", "Iterb",   173.04f,   8, 16, LAN, "[Xe] 4f¹⁴ 6s²",    "Lantanowce"),
    Element(71, "Lu", "Lutet",   174.967f,  8, 17, LAN, "[Xe] 4f¹⁴ 5d¹ 6s²","Lantanowce"),
    // Period 6 continued (Hf–Rn)
    Element(72, "Hf", "Hafn",    178.49f,   6,  4, TM,  "[Xe] 4f¹⁴ 5d² 6s²",""),
    Element(73, "Ta", "Tantal",  180.948f,  6,  5, TM,  "[Xe] 4f¹⁴ 5d³ 6s²",""),
    Element(74, "W",  "Wolfram", 183.84f,   6,  6, TM,  "[Xe] 4f¹⁴ 5d⁴ 6s²",""),
    Element(75, "Re", "Ren",     186.207f,  6,  7, TM,  "[Xe] 4f¹⁴ 5d⁵ 6s²",""),
    Element(76, "Os", "Osm",     190.23f,   6,  8, TM,  "[Xe] 4f¹⁴ 5d⁶ 6s²",""),
    Element(77, "Ir", "Iryd",    192.217f,  6,  9, TM,  "[Xe] 4f¹⁴ 5d⁷ 6s²",""),
    Element(78, "Pt", "Platyna", 195.084f,  6, 10, TM,  "[Xe] 4f¹⁴ 5d⁹ 6s¹",""),
    Element(79, "Au", "Złoto",   196.967f,  6, 11, TM,  "[Xe] 4f¹⁴ 5d¹⁰ 6s¹",""),
    Element(80, "Hg", "Rtęć",    200.592f,  6, 12, TM,  "[Xe] 4f¹⁴ 5d¹⁰ 6s²",""),
    Element(81, "Tl", "Tal",     204.383f,  6, 13, PT,  "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p¹","Borowce"),
    Element(82, "Pb", "Ołów",    207.2f,    6, 14, PT,  "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p²","Węglowce"),
    Element(83, "Bi", "Bizmut",  208.980f,  6, 15, PT,  "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p³","Azotowce"),
    Element(84, "Po", "Polon",   209.0f,    6, 16, ME,  "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p⁴","Tlenowce"),
    Element(85, "At", "Astat",   210.0f,    6, 17, HA,  "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p⁵","Halogeny"),
    Element(86, "Rn", "Radon",   222.0f,    6, 18, NG,  "[Xe] 4f¹⁴ 5d¹⁰ 6s² 6p⁶","Gazy szlachetne"),
    // Period 7
    Element(87, "Fr", "Frans",   223.0f,    7,  1, AM,  "[Rn] 7s¹",         "Litowce"),
    Element(88, "Ra", "Rad",     226.0f,    7,  2, AE,  "[Rn] 7s²",         "Berylowce"),
    Element(89, "Ac", "Aktyn",   227.0f,    7,  3, ACT, "[Rn] 6d¹ 7s²",     "Aktynowce"),
    // Actinides — tableRow=9, tableCol=4..17
    Element(90,  "Th", "Tor",        232.038f, 9,  4, ACT, "[Rn] 6d² 7s²",      "Aktynowce"),
    Element(91,  "Pa", "Protaktyn",  231.036f, 9,  5, ACT, "[Rn] 5f² 6d¹ 7s²", "Aktynowce"),
    Element(92,  "U",  "Uran",       238.029f, 9,  6, ACT, "[Rn] 5f³ 6d¹ 7s²", "Aktynowce"),
    Element(93,  "Np", "Neptun",     237.0f,   9,  7, ACT, "[Rn] 5f⁴ 6d¹ 7s²", "Aktynowce"),
    Element(94,  "Pu", "Pluton",     244.0f,   9,  8, ACT, "[Rn] 5f⁶ 7s²",     "Aktynowce"),
    Element(95,  "Am", "Ameryk",     243.0f,   9,  9, ACT, "[Rn] 5f⁷ 7s²",     "Aktynowce"),
    Element(96,  "Cm", "Kiur",       247.0f,   9, 10, ACT, "[Rn] 5f⁷ 6d¹ 7s²", "Aktynowce"),
    Element(97,  "Bk", "Berkel",     247.0f,   9, 11, ACT, "[Rn] 5f⁹ 7s²",     "Aktynowce"),
    Element(98,  "Cf", "Kaliforn",   251.0f,   9, 12, ACT, "[Rn] 5f¹⁰ 7s²",    "Aktynowce"),
    Element(99,  "Es", "Einstein",   252.0f,   9, 13, ACT, "[Rn] 5f¹¹ 7s²",    "Aktynowce"),
    Element(100, "Fm", "Ferm",       257.0f,   9, 14, ACT, "[Rn] 5f¹² 7s²",    "Aktynowce"),
    Element(101, "Md", "Mendeleew",  258.0f,   9, 15, ACT, "[Rn] 5f¹³ 7s²",    "Aktynowce"),
    Element(102, "No", "Nobel",      259.0f,   9, 16, ACT, "[Rn] 5f¹⁴ 7s²",    "Aktynowce"),
    Element(103, "Lr", "Lorens",     266.0f,   9, 17, ACT, "[Rn] 5f¹⁴ 7s²",    "Aktynowce"),
    // Period 7 continued (Rf–Og)
    Element(104, "Rf", "Rutherford", 267.0f,   7,  4, TM,  "",                  ""),
    Element(105, "Db", "Dubn",       268.0f,   7,  5, TM,  "",                  ""),
    Element(106, "Sg", "Seaborg",    271.0f,   7,  6, TM,  "",                  ""),
    Element(107, "Bh", "Bohr",       272.0f,   7,  7, TM,  "",                  ""),
    Element(108, "Hs", "Has",        270.0f,   7,  8, TM,  "",                  ""),
    Element(109, "Mt", "Meitner",    276.0f,   7,  9, TM,  "",                  ""),
    Element(110, "Ds", "Darmsztad",  281.0f,   7, 10, TM,  "",                  ""),
    Element(111, "Rg", "Roentgen",   280.0f,   7, 11, TM,  "",                  ""),
    Element(112, "Cn", "Kopernik",   285.0f,   7, 12, TM,  "",                  ""),
    Element(113, "Nh", "Nihon",      284.0f,   7, 13, PT,  "",                  ""),
    Element(114, "Fl", "Flerow",     289.0f,   7, 14, PT,  "",                  ""),
    Element(115, "Mc", "Moskow",     288.0f,   7, 15, PT,  "",                  ""),
    Element(116, "Lv", "Livermorium",293.0f,   7, 16, PT,  "",                  ""),
    Element(117, "Ts", "Tennesse",   294.0f,   7, 17, HA,  "",                  ""),
    Element(118, "Og", "Oganesson",  294.0f,   7, 18, NG,  "",                  "Gazy szlachetne"),
)

val elementByNumber: Map<Int, Element> = ELEMENTS.associateBy { it.atomicNumber }

/** Simple shell (K/L/M/N…) electron count notation, e.g. "2,8,1" for Na */
val shellConfigByNumber: Map<Int, String> = mapOf(
    1  to "1",
    2  to "2",
    3  to "2,1",
    4  to "2,2",
    5  to "2,3",
    6  to "2,4",
    7  to "2,5",
    8  to "2,6",
    9  to "2,7",
    10 to "2,8",
    11 to "2,8,1",
    12 to "2,8,2",
    13 to "2,8,3",
    14 to "2,8,4",
    15 to "2,8,5",
    16 to "2,8,6",
    17 to "2,8,7",
    18 to "2,8,8",
    19 to "2,8,8,1",
    20 to "2,8,8,2",
    21 to "2,8,9,2",
    22 to "2,8,10,2",
    23 to "2,8,11,2",
    24 to "2,8,13,1",
    25 to "2,8,13,2",
    26 to "2,8,14,2",
    27 to "2,8,15,2",
    28 to "2,8,16,2",
    29 to "2,8,18,1",
    30 to "2,8,18,2",
    31 to "2,8,18,3",
    32 to "2,8,18,4",
    33 to "2,8,18,5",
    34 to "2,8,18,6",
    35 to "2,8,18,7",
    36 to "2,8,18,8",
    37 to "2,8,18,8,1",
    38 to "2,8,18,8,2",
    47 to "2,8,18,18,1",
    53 to "2,8,18,18,7",
    54 to "2,8,18,18,8",
    79 to "2,8,18,32,18,1",
)
