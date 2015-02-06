SELECT
    Name
FROM
    Product p,
    Chemical c
WHERE
    p.ReactionWID = ?
    && p.OtherWID = c.WID;