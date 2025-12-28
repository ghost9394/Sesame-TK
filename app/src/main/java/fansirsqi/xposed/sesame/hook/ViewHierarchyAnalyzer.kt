package fansirsqi.xposed.sesame.hook

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import fansirsqi.xposed.sesame.hook.simple.SimpleViewImage



/**
 * A utility object for analyzing and traversing the view hierarchy.
 */
object ViewHierarchyAnalyzer {

    private const val TAG = "ViewHierarchyAnalyzer"

    /**
     * Recursively logs the view hierarchy for debugging purposes.
     * @param view The root view to start the analysis from.
     * @param depth The current depth of recursion for formatting.
     */
    fun logViewHierarchy(view: View, depth: Int) {
        val indent = "  ".repeat(depth)
        val className = view.javaClass.name
        val resourceId = try {
            "ID: ${view.resources.getResourceEntryName(view.id)}"
        } catch (e: Exception) {
            "ID: (none)"
        }
        val location = IntArray(2).also { view.getLocationOnScreen(it) }
        val info = "loc=[${location[0]},${location[1]}] size=[${view.width}x${view.height}] visible=${view.isShown} enabled=${view.isEnabled}"
        var textInfo = ""
        if (view is TextView) {
            textInfo = "text='${view.text}' desc='${view.contentDescription}'"
        }

        Log.d(TAG, "$indent- $className, $resourceId, $info $textInfo")

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                logViewHierarchy(child, depth + 1)
            }
        }
    }

    /**
     * Finds the actual slider button view by traversing from an anchor text view.
     * It logs the hierarchy for debugging on the first attempt.
     * @param slideTextView The SimpleViewImage wrapper for the "slide to verify" text view.
     * @return The found slider View, or null if not found.
     */
    fun findActualSliderView(slideTextView: SimpleViewImage): View? {
        val parentView = slideTextView.originView.parent as? ViewGroup ?: return null

        Log.d(TAG, "========= Analyzing slider parent view hierarchy =========")
        logViewHierarchy(parentView, 0)
        Log.d(TAG, "========= End of view hierarchy analysis =========")

        // Start a recursive search for the slider view within the parent container.
        val slider = findSliderInGroup(parentView)
        if (slider != null) {
            val loc = IntArray(2).also { slider.getLocationOnScreen(it) }
            Log.d(TAG, "Found draggable slider view: ${slider.javaClass.name} at loc=[${loc[0]},${loc[1]}]")
        } else {
            Log.e(TAG, "Could not find the actual slider view. Please check the hierarchy logs above.")
        }
        return slider
    }

    /**
     * Recursively searches for a candidate slider view within a ViewGroup.
     * The strategy is to find a visible ImageView (the icon) and return its parent (the actual draggable view).
     * @param viewGroup The group to search within.
     * @return The found slider View, or null.
     */
    private fun findSliderInGroup(viewGroup: ViewGroup): View? {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            // The draggable part is the parent of the visible ImageView icon.
            if (child is ImageView && child.isShown) {
                Log.d(TAG, "Found slider icon (ImageView). Returning its parent as the draggable view.")
                return child.parent as? View
            }

            // If not found, recurse into sub-ViewGroups.
            if (child is ViewGroup) {
                val foundInChild = findSliderInGroup(child)
                if (foundInChild != null) {
                    return foundInChild
                }
            }
        }
        return null
    }
}
