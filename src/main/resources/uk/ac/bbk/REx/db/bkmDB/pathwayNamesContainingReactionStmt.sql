SELECT p.pathwayName
FROM pathways p, reactionsToPathways rtp
WHERE p.pathwayID=rtp.pathwayID
	AND rtp.reactionID=?
ORDER BY p.pathwayName