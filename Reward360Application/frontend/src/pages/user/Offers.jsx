import React, { useState, useEffect, useCallback } from "react";
import { useUser } from "../../context/UserContext";
import "../../styles/Offer.css";

export default function Offers() {
  const { user, offers, redeemOffer, loading, redemptions, setOffers } =
    useUser();
  const [confirm, setConfirm] = useState(null);
  const [category, setCategory] = useState("All");

  // Function to fetch latest offers
  const fetchOffers = useCallback(async () => {
    try {
      const response = await fetch("http://localhost:8080/api/offers");
      if (response.ok) {
        const data = await response.json();
        if (setOffers) {
          setOffers(data);
        }
      }
    } catch (error) {
      console.error("Error fetching offers:", error);
    }
  }, [setOffers]);

  // Auto-refresh when tab becomes visible or window gets focus
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        fetchOffers();
      }
    };

    const handleFocus = () => {
      fetchOffers();
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);
    window.addEventListener("focus", handleFocus);

    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      window.removeEventListener("focus", handleFocus);
    };
  }, [fetchOffers]);

  // Fetch on mount
  useEffect(() => {
    fetchOffers();
  }, [fetchOffers]);

  if (loading || !user) return <div className="offers-page">Loading...</div>;

  const open = (o) => setConfirm(o);
  const close = () => setConfirm(null);

  const redeemedTitles = new Set((redemptions || []).map((r) => r.offerTitle));

  const redeem = async () => {
    if (!confirm) return;
    try {
      await redeemOffer(confirm.id, "Online");
      setConfirm(null);
      alert("Redemption confirmed! Check Redemptions page.");
    } catch (error) {
      alert("Failed to redeem offer. Please try again.");
    }
  };

  return (
    <div className="offers-page">
      <div className="o-hero">
        <div className="o-hero-overlay">
          <div className="o-hero-row">
            <div>
              <h3 className="o-hero-title">Member Offers</h3>
              <div className="o-hero-sub">
                <span className="o-user-chip">
                  <span className="o-user-dot" />
                  {user.customerName}
                </span>
                <span className="o-sep">•</span>
                <span className="o-tier">
                  Tier:&nbsp;
                  <strong>{user.loyaltyTier || "Bronze"}</strong>
                </span>
              </div>
            </div>

            <div
              className="o-hero-balance-badge"
              title="Current Points Balance"
            >
              <span className="o-balance-label">Balance</span>
              <span className="o-balance-value">{user.pointsBalance ?? 0}</span>
              <span className="o-balance-unit">pts</span>
            </div>
          </div>
        </div>
      </div>

      <div
        style={{
          display: "flex",
          justifyContent: "flex-end",
          alignItems: "center",
          gap: 12,
          margin: "18px 0",
          width: "100%",
        }}
      >
        <label style={{ fontWeight: 600 }}>Category:</label>
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          style={{
            padding: "6px 10px",
            borderRadius: 6,
            border: "1px solid #ccc",
          }}
        >
          <option value="All">All</option>
          {Array.from(
            new Set(offers.map((o) => o.category).filter(Boolean)),
          ).map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      <div className="o-grid">
        {offers
          .filter(
            (o) => category === "All" || !o.category || o.category === category,
          )
          .map((o) => (
            <div className="o-card o-offer" key={o.id}>
              <div className="o-img-wrap">
                <img className="o-img" src={o.imageUrl} alt={o.title} />
              </div>
              <div className="o-body">
                <h4 className="o-card-title">{o.title}</h4>
                <p className="o-desc">{o.description}</p>
                <div className="o-row">
                  <span className="o-pill">
                    Cost: <strong>{o.costPoints}</strong> pts
                  </span>
                  {redeemedTitles.has(o.title) ? (
                    <button
                      className="o-btn"
                      disabled
                      style={{ opacity: 0.6, cursor: "not-allowed" }}
                    >
                      Redeemed
                    </button>
                  ) : (
                    <button
                      className="o-btn"
                      onClick={() => open(o)}
                      disabled={user.pointsBalance < o.costPoints}
                      style={{
                        opacity: user.pointsBalance < o.costPoints ? 0.6 : 1,
                      }}
                    >
                      Redeem
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
      </div>

      {confirm && (
        <div className="o-modal-backdrop" onClick={close} aria-hidden="true">
          <div
            className="o-modal-card o-card"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="o-modal-grid">
              <div className="o-modal-img-wrap">
                <img
                  className="o-modal-img"
                  src={confirm.imageUrl}
                  alt={confirm.title}
                />
              </div>
              <div className="o-modal-body">
                <h4 className="o-card-title">{confirm.title}</h4>
                <p className="o-desc">{confirm.description}</p>
                <div className="o-modal-meta">
                  <div className="o-meta-row">
                    <span className="o-meta-label">Cost</span>
                    <span className="o-meta-value">
                      {confirm.costPoints} pts
                    </span>
                  </div>
                </div>
                <div className="o-modal-actions">
                  <button className="o-btn" onClick={redeem}>
                    Confirm &amp; Redeem
                  </button>
                  <button className="o-btn o-btn-ghost" onClick={close}>
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
