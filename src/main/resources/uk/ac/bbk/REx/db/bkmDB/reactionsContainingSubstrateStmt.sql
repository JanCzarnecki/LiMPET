SELECT s.reactionID
FROM substrates s, compounds c
WHERE s.compoundID=c.compoundID
	AND c.inchi=?
ORDER BY s.reactionID