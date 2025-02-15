package de.westnordost.streetcomplete.quests.building_entrance

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.osmquests.OsmElementQuestType
import de.westnordost.streetcomplete.data.osm.osmquests.Tags
import de.westnordost.streetcomplete.data.user.achievements.QuestTypeAchievement.PEDESTRIAN

class AddEntrance : OsmElementQuestType<EntranceAnswer> {

    private val withoutEntranceFilter by lazy { """
        nodes with
          !entrance and !barrier and noexit != yes
    """.toElementFilterExpression() }

    private val buildingWaysFilter by lazy { """
        ways, relations with building and building !~ yes|no|service|shed|house|detached|terrace|semi|semidetached_house
    """.toElementFilterExpression() }

    private val incomingWaysFilter by lazy { """
        ways with
          highway ~ path|footway and area != yes and access !~ private|no
    """.toElementFilterExpression() }

    private val excludedWaysFilter by lazy { """
        ways with (tunnel and tunnel != no) or covered=yes
    """.toElementFilterExpression() }

    override val changesetComment = "Add entrance info"
    override val wikiLink = "Key:entrance"
    override val icon = R.drawable.ic_quest_door
    override val questTypeAchievements = listOf(PEDESTRIAN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_building_entrance_title

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> {
        val buildingsWayNodeIds = mutableSetOf<Long>()
        mapData.ways
            .filter { buildingWaysFilter.matches(it) }
            .flatMapTo(buildingsWayNodeIds) { it.nodeIds }

        val incomingWayNodeIds = mutableSetOf<Long>()
        mapData.ways
            .filter { incomingWaysFilter.matches(it) }
            .flatMapTo(incomingWayNodeIds) { it.nodeIds }

        val excludedWayNodeIds = mutableSetOf<Long>()
        mapData.ways
            .filter { excludedWaysFilter.matches(it) }
            .flatMapTo(excludedWayNodeIds) { it.nodeIds }

        return mapData.nodes
            .filter { it.id in buildingsWayNodeIds && it.id in incomingWayNodeIds
                && it.id !in excludedWayNodeIds
                && withoutEntranceFilter.matches(it)
            }
    }

    override fun isApplicableTo(element: Element): Boolean? =
        if (!withoutEntranceFilter.matches(element)) false else null

    override fun createForm() = AddEntranceForm()

    override fun applyAnswerTo(answer: EntranceAnswer, tags: Tags, timestampEdited: Long) {
        when (answer) {
            DeadEnd -> tags["noexit"] = "yes"
            is EntranceExistsAnswer -> tags["entrance"] = answer.osmValue
        }
    }
}
