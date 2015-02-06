SELECT
    Name
FROM
    Reactant r,
    Chemical c
WHERE
    r.ReactionWID = ?
    && r.OtherWID = c.WID;