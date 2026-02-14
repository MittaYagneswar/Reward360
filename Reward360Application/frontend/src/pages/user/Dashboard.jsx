import React, { useState, useEffect, useCallback } from "react";
import { useUser } from "../../context/UserContext";
import "../../styles/Dashboard.css";

function ClaimCard({ a, onClaimed, isClaimed }) {
  const [busy, setBusy] = useState(false);
  const { claimPoints } = useUser();

  const claim = async () => {
    setBusy(true);
    try {
      await claimPoints(a.code, a.points, a.title);
      onClaimed(a.code);
    } catch (error) {
      console.error("Failed to claim:", error);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="d-card d-claim-card">
      <h4 className="d-card-title">{a.title}</h4>
      <p>Earn {a.points} points</p>
      {!isClaimed ? (
        <button disabled={busy} className="d-btn" onClick={claim}>
          {busy ? "Claiming…" : "Claim"}
        </button>
      ) : (
        <span className="d-badge">Claimed</span>
      )}
    </div>
  );
}

export default function Dashboard() {
  const { user, transactions, loading, setUser, setTransactions } = useUser();
  const [claimedActivities, setClaimedActivities] = useState([]);

  const userStorageKey = user ? `claimedActivities_${user.userId}` : null;

  // Function to fetch latest user data
  const fetchUserData = useCallback(async () => {
    if (!user?.userId) return;
    try {
      // Fetch latest user data
      const userResponse = await fetch(
        `http://localhost:8080/api/customers/${user.userId}`,
      );
      if (userResponse.ok) {
        const userData = await userResponse.json();
        if (setUser) {
          setUser(userData);
        }
      }

      // Fetch latest transactions
      const transResponse = await fetch(
        `http://localhost:8080/api/transactions/customer/${user.userId}`,
      );
      if (transResponse.ok) {
        const transData = await transResponse.json();
        if (setTransactions) {
          setTransactions(transData);
        }
      }
    } catch (error) {
      console.error("Error fetching user data:", error);
    }
  }, [user?.userId, setUser, setTransactions]);

  // Auto-refresh when tab becomes visible or window gets focus
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        fetchUserData();
      }
    };

    const handleFocus = () => {
      fetchUserData();
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);
    window.addEventListener("focus", handleFocus);

    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      window.removeEventListener("focus", handleFocus);
    };
  }, [fetchUserData]);

  useEffect(() => {
    if (userStorageKey) {
      const saved = sessionStorage.getItem(userStorageKey);
      setClaimedActivities(saved ? JSON.parse(saved) : []);
    }
  }, [userStorageKey]);

  useEffect(() => {
    if (userStorageKey) {
      sessionStorage.setItem(userStorageKey, JSON.stringify(claimedActivities));
    }
  }, [claimedActivities, userStorageKey]);

  const handleClaimed = (code) => {
    setClaimedActivities((prev) => [...prev, code]);
  };

  if (loading || !user) return <div className="d-page">Loading...</div>;

  const activities = [
    { title: "Daily Login Bonus", points: 50, code: "LOGIN" },
    { title: "Write a Product Review", points: 100, code: "REVIEW" },
    { title: "Share on Social", points: 75, code: "SOCIAL" },
    { title: "Refer a Friend", points: 200, code: "REFER" },
  ];

  const nextExpiry = user.nextExpiry ?? "23/2/2026";
  const today = new Date().toISOString().split("T")[0];
  const todayTransactions = transactions.filter((t) => t.date === today);
  const pointsEarnedToday = todayTransactions.reduce(
    (sum, t) => sum + (t.pointsEarned || 0),
    0,
  );
  const pointsRedeemedToday = todayTransactions.reduce(
    (sum, t) => sum + (t.pointsRedeemed || 0),
    0,
  );
  const totalPointsEarned = user.lifetimePoints ?? 0;

  return (
    <div className="d-page">
      <div className="d-card d-ps">
        <div className="d-ps-row">
          <div className="d-ps-left">
            <h3 className="d-ps-title">Points Summary</h3>
            <p className="d-ps-sub">
              Member: <span className="d-ps-strong">{user.customerName}</span>
              <span className="d-ps-dot">·</span>
              Tier: <span className="d-ps-strong">{user.loyaltyTier}</span>
            </p>
            <div className="d-ps-pill">
              <span className="d-ps-pill-label">Next Expiry</span>
              <span className="d-ps-pill-value">{nextExpiry}</span>
            </div>
          </div>
          <div className="d-ps-right">
            <div className="d-ps-right-label">Current Balance</div>
            <div className="d-ps-right-value">{user.pointsBalance ?? 0}</div>
          </div>
        </div>
      </div>

      <div className="d-stats-row">
        <div className="d-stat-card">
          <div className="d-stat-icon">📊</div>
          <div className="d-stat-content">
            <div className="d-stat-label">Total Earned</div>
            <div className="d-stat-value">{totalPointsEarned}</div>
          </div>
        </div>
        <div className="d-stat-card">
          <div className="d-stat-icon">⬆️</div>
          <div className="d-stat-content">
            <div className="d-stat-label">Earned Today</div>
            <div className="d-stat-value d-stat-earned">
              {pointsEarnedToday}
            </div>
          </div>
        </div>
        <div className="d-stat-card">
          <div className="d-stat-icon">⬇️</div>
          <div className="d-stat-content">
            <div className="d-stat-label">Redeemed Today</div>
            <div className="d-stat-value d-stat-redeemed">
              {pointsRedeemedToday}
            </div>
          </div>
        </div>
      </div>

      <div className="d-card">
        <h3 className="d-section-title">Daily Activities</h3>
        <div className="d-activities-row">
          {activities.map((a) => (
            <ClaimCard
              key={a.code}
              a={a}
              onClaimed={handleClaimed}
              isClaimed={claimedActivities.includes(a.code)}
            />
          ))}
        </div>
      </div>

      <div className="d-card">
        <h3 className="d-section-title">Recent Transactions</h3>
        <div className="d-table-wrap">
          <table className="d-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Points Earned</th>
                <th>Points Redeemed</th>
                <th>Store/Activity</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((t) => (
                <tr key={t.id}>
                  <td>{t.date}</td>
                  <td>{t.type}</td>
                  <td style={{ color: "#059669", fontWeight: "700" }}>
                    {t.pointsEarned || "—"}
                  </td>
                  <td style={{ color: "#dc2626", fontWeight: "700" }}>
                    {t.pointsRedeemed || "—"}
                  </td>
                  <td>{t.store || t.note}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
