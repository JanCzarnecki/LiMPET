SELECT p.reactionID
FROM products p, compounds c
WHERE p.compoundID=c.compoundID
	AND c.inchi=?
ORDER By p.reactionID